# Star Imports Solution: Using FIR Import Scopes

## Problem Statement

Currently, the java-direct implementation handles:
- ✅ Fully qualified names: `java.util.ArrayList` → works
- ✅ Single-type imports: `import java.util.ArrayList;` → works (handled in Java Model)
- ❌ `java.lang.*` automatic import: `Object` → fails (not resolved to `java.lang.Object`)
- ❌ Star imports: `import java.util.*;` then `List` → fails (not resolved to `java.util.List`)

The proposed solution addresses both remaining issues by leveraging FIR's existing import scope infrastructure in the `classifier == null` fallback path of `JavaTypeConversion.kt`.

## Architecture Overview

### Current Architecture

```
Java Source File
       ↓
Java Model (java-direct)
  - Returns simple names: "Object", "List"
  - Handles single-type imports only
       ↓
FIR JavaTypeConversion.kt
  - toConeKotlinTypeForFlexibleBound()
  - When classifier == null:
    - Creates ClassId.topLevel(FqName("Object"))
    - Looks in root package → FAILS
```

### Proposed Architecture

```
Java Source File
       ↓
Java Model (java-direct)
  - Returns simple names: "Object", "List"
  - Handles single-type imports only
  - NEW: Exposes star imports information
       ↓
FIR JavaTypeConversion.kt
  - toConeKotlinTypeForFlexibleBound()
  - When classifier == null:
    - NEW: For simple names, try resolution with star imports:
      1. Check java.lang.* (automatic import per JLS §7.5.5)
      2. Check explicit star imports from Java file
      3. Fall back to root package (current behavior)
```

## FIR Import Scope Infrastructure

FIR has a well-established import scope system used for Kotlin files:

### Key Classes

1. **`FirResolvedImport`** (`compiler/fir/tree/gen/org/jetbrains/kotlin/fir/declarations/FirResolvedImport.kt`)
   - Represents a resolved import directive
   - Properties:
     - `importedFqName: FqName?` - the imported package/class FQN
     - `isAllUnder: Boolean` - true for star imports
     - `packageFqName: FqName` - package portion of the import
     - `resolvedParentClassId: ClassId?` - for static imports

2. **`FirAbstractStarImportingScope`** (`compiler/fir/providers/src/org/jetbrains/kotlin/fir/scopes/impl/FirAbstractStarImportingScope.kt`)
   - Base class for scopes that resolve names from star imports
   - Key method: `processClassifiersByNameWithSubstitution(name, processor)`
   - Uses `processClassifiersFromImportsByName()` to iterate through star imports

3. **`FirAbstractImportingScope`** (`compiler/fir/providers/src/org/jetbrains/kotlin/fir/scopes/impl/FirAbstractImportingScope.kt`)
   - Contains logic for resolving classifiers from imports
   - `processClassifiersFromImportsByName()` method:
     ```kotlin
     protected fun processClassifiersFromImportsByName(
         name: Name?,
         imports: List<FirResolvedImport>,
         processor: (FirClassLikeSymbol<*>) -> Unit
     ) {
         for (import in imports) {
             val importedName = name ?: import.importedName ?: continue
             if (isExcluded(import, importedName)) continue
             val classId = import.resolvedParentClassId?.createNestedClassId(importedName)
                 ?: ClassId.topLevel(import.packageFqName.child(importedName))
             val symbol = provider.getClassLikeSymbolByClassId(classId) ?: continue
             processor(symbol)
         }
     }
     ```

### How Star Import Resolution Works in FIR

For Kotlin files with star imports:
```kotlin
import java.util.*

val list: List<String> = ...
```

1. Parser creates `FirImport` declarations
2. `FirImportResolveTransformer` resolves them to `FirResolvedImport`
3. `FirExplicitStarImportingScope` is created with star imports list
4. When resolving `List` type:
   - Scope calls `processClassifiersByNameWithSubstitution("List", ...)`
   - Iterates through star imports
   - For `import java.util.*`: tries `ClassId.topLevel(FqName("java.util").child(Name("List")))`
   - Finds `java.util.List` via `provider.getClassLikeSymbolByClassId()`

## Proposed Solution

### Option B.1: Extend JavaClassifierType Interface

**Approach:** Add star imports information to JavaClassifierType interface.

#### Changes Required

**1. Extend `JavaClassifierType` interface** (`compiler/frontend.java/src/org/jetbrains/kotlin/load/java/structure/JavaClassifierType.java`):

```java
public interface JavaClassifierType extends JavaType {
    @Nullable
    JavaClassifier getClassifier();
    
    @NotNull
    String getClassifierQualifiedName();
    
    // NEW: Expose star imports from the containing file
    @NotNull
    default List<String> getStarImportPackages() {
        return Collections.emptyList();
    }
}
```

**2. Implement in java-direct** (`JavaClassifierTypeOverAst.kt`):

```kotlin
class JavaClassifierTypeOverAst(
    private val node: AstJavaType,
    private val localScope: LocalJavaScope?,
    private val imports: AstJavaImports?,
    private val typeParameters: List<JavaTypeParameter>
) : JavaClassifierType {
    // ... existing code ...
    
    override fun getStarImportPackages(): List<String> {
        // Return list of star-imported package FQNs
        return imports?.starImports?.map { it.asString() } ?: emptyList()
    }
}
```

**3. Modify JavaTypeConversion.kt** (`compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`):

```kotlin
null -> {
    val qualifiedName = this.classifierQualifiedName
    val classId = if (!qualifiedName.contains('.')) {
        // Simple name - try resolution with star imports
        resolveSimpleNameWithStarImports(qualifiedName, this.starImportPackages, session)
    } else {
        // Already qualified
        ClassId.topLevel(FqName(qualifiedName))
    }
    classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
}

private fun resolveSimpleNameWithStarImports(
    simpleName: String,
    starImportPackages: List<String>,
    session: FirSession
): ClassId {
    val name = Name.identifier(simpleName)
    
    // 1. Check java.lang first (automatic import per JLS §7.5.5)
    val javaLangId = ClassId(FqName("java.lang"), name)
    if (session.symbolProvider.getClassLikeSymbolByClassId(javaLangId) != null) {
        return javaLangId
    }
    
    // 2. Check explicit star imports
    for (packageFqName in starImportPackages) {
        val classId = ClassId(FqName(packageFqName), name)
        if (session.symbolProvider.getClassLikeSymbolByClassId(classId) != null) {
            return classId
        }
    }
    
    // 3. Fall back to root package (maintains backward compatibility)
    return ClassId.topLevel(FqName(simpleName))
}
```

**Pros:**
- Clean interface extension
- Minimal changes to FIR code (~20 lines)
- Reuses existing FIR symbol provider
- Self-contained in java-direct module
- No architectural changes to FIR

**Cons:**
- Modifies JavaModel interface (affects PSI and javac-wrapper)
  - But default implementation returns empty list, so no impact
- Doesn't reuse FirAbstractStarImportingScope (implements similar logic)

### Option B.2: Create FirResolvedImport List in JavaModel

**Approach:** Make JavaModel return FirResolvedImport objects for star imports.

#### Changes Required

**1. Extend `JavaClassifierType` interface**:

```java
public interface JavaClassifierType extends JavaType {
    @Nullable
    JavaClassifier getClassifier();
    
    @NotNull
    String getClassifierQualifiedName();
    
    // NEW: Expose resolved star imports for FIR
    @NotNull
    default List<?> getStarImportsForFir() {  // List<FirResolvedImport>
        return Collections.emptyList();
    }
}
```

**2. Implement in java-direct**:

```kotlin
class JavaClassifierTypeOverAst(
    private val node: AstJavaType,
    private val localScope: LocalJavaScope?,
    private val imports: AstJavaImports?,
    private val typeParameters: List<JavaTypeParameter>,
    private val session: FirSession  // NEW: Need FirSession access
) : JavaClassifierType {
    override fun getStarImportsForFir(): List<FirResolvedImport> {
        return imports?.starImports?.map { packageFqName ->
            buildResolvedImport {
                delegate = buildImport {
                    importedFqName = packageFqName
                    isAllUnder = true
                }
                this.packageFqName = packageFqName
                relativeParentClassName = null
            }
        } ?: emptyList()
    }
}
```

**3. Modify JavaTypeConversion.kt** to use `FirAbstractImportingScope` logic:

```kotlin
null -> {
    val qualifiedName = this.classifierQualifiedName
    val classId = if (!qualifiedName.contains('.')) {
        val name = Name.identifier(qualifiedName)
        val starImports = (this.starImportsForFir as List<FirResolvedImport>) + createJavaLangStarImport()
        
        resolveClassifierFromStarImports(name, starImports, session.symbolProvider)
    } else {
        ClassId.topLevel(FqName(qualifiedName))
    }
    classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
}

private fun resolveClassifierFromStarImports(
    name: Name,
    starImports: List<FirResolvedImport>,
    symbolProvider: FirSymbolProvider
): ClassId {
    // Reuse FirAbstractImportingScope logic
    for (import in starImports) {
        val classId = ClassId.topLevel(import.packageFqName.child(name))
        if (symbolProvider.getClassLikeSymbolByClassId(classId) != null) {
            return classId
        }
    }
    // Fall back to root package
    return ClassId.topLevel(FqName(name.asString()))
}

private fun createJavaLangStarImport(): FirResolvedImport {
    return buildResolvedImport {
        delegate = buildImport {
            importedFqName = FqName("java.lang")
            isAllUnder = true
        }
        packageFqName = FqName("java.lang")
        relativeParentClassName = null
    }
}
```

**Pros:**
- Reuses FIR's data structures
- More aligned with Kotlin file handling
- Could enable more sophisticated import features later

**Cons:**
- Requires FirSession access in JavaModel (violates architecture)
- More complex implementation
- Creates FIR objects in JavaModel layer (wrong abstraction level)

### Option B.3: Minimal - Just Add java.lang Check

**Approach:** Only handle `java.lang.*` automatic import in FIR, ignore explicit star imports initially.

**Rationale:** 
- Java code rarely uses star imports (discouraged by style guides)
- `java.lang.*` is by far the most common case (automatic per JLS)
- Simple implementation, can add full star import support later

#### Changes Required

**No changes to JavaModel interface** - use existing code.

**Only modify JavaTypeConversion.kt**:

```kotlin
null -> {
    val qualifiedName = this.classifierQualifiedName
    val classId = if (!qualifiedName.contains('.')) {
        // Simple name - try java.lang first (automatic import per JLS §7.5.5)
        val name = Name.identifier(qualifiedName)
        val javaLangId = ClassId(FqName("java.lang"), name)
        
        if (session.symbolProvider.getClassLikeSymbolByClassId(javaLangId) != null) {
            javaLangId
        } else {
            // Fall back to root package
            ClassId.topLevel(FqName(qualifiedName))
        }
    } else {
        // Already qualified
        ClassId.topLevel(FqName(qualifiedName))
    }
    classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
}
```

**Pros:**
- Simplest implementation (~10 lines)
- No interface changes
- Solves 95% of the problem (java.lang types)
- No architectural concerns
- Can be done in a single iteration

**Cons:**
- Doesn't handle explicit star imports (rare but valid Java code)
- Will need follow-up work for complete star import support

## Comparison Matrix

| Aspect | B.1: Package List | B.2: FirResolvedImport | B.3: java.lang Only |
|--------|-------------------|------------------------|---------------------|
| **Completeness** | Full | Full | Partial (95%) |
| **Complexity** | Medium | High | Low |
| **Lines Changed** | ~50 | ~80 | ~10 |
| **Interface Changes** | Yes (with default) | Yes (with default) | No |
| **Architecture Impact** | Minimal | Violates layering | None |
| **FirSession in JavaModel** | No | Yes (bad) | No |
| **Iteration Time** | 1-2 hours | 2-3 hours | 30 minutes |
| **Risk** | Low | Medium | Very Low |

## Recommendation

### Phase 1: Implement Option B.3 (java.lang only)

**Reason:** 
- Solves the immediate problem (127 test failures → expect ~80-100 passing)
- Minimal risk and implementation time
- No architectural concerns
- Can be done in current iteration

**Implementation:**
1. Modify `JavaTypeConversion.kt` (10 lines)
2. Run tests
3. Update `ITERATION_RESULTS.md`
4. Commit with message: "java-direct: Add java.lang.* automatic import resolution"

### Phase 2: Add Full Star Import Support (Option B.1)

**After Phase 1 is validated:**
- Extend JavaClassifierType with `getStarImportPackages()` default method
- Implement in java-direct
- Update JavaTypeConversion.kt to check explicit star imports
- Run tests on Java code using star imports

**Why not immediately:**
- Separate concerns (java.lang vs. explicit imports)
- Validate architecture incrementally
- Each phase adds measurable value

## Implementation Details for Phase 1

### File to Modify

`compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`

### Exact Change (lines 248-251)

**Before:**
```kotlin
        null -> {
            val classId = ClassId.topLevel(FqName(this.classifierQualifiedName))
            classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
        }
```

**After:**
```kotlin
        null -> {
            val qualifiedName = this.classifierQualifiedName
            val classId = if (!qualifiedName.contains('.')) {
                // Simple name - try java.lang.* first (automatic import per JLS §7.5.5)
                val name = Name.identifier(qualifiedName)
                val javaLangId = ClassId(FqName("java.lang"), name)
                
                if (session.symbolProvider.getClassLikeSymbolByClassId(javaLangId) != null) {
                    javaLangId
                } else {
                    ClassId.topLevel(FqName(qualifiedName))
                }
            } else {
                ClassId.topLevel(FqName(qualifiedName))
            }
            classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
        }
```

### Test Validation

Run box tests:
```bash
./gradlew :compiler:tests-for-compiler-generator:test \
  --tests "org.jetbrains.kotlin.test.runners.codegen.BlackBoxCodegenForJavaDirectSuppressionTestGenerated" \
  -q
```

**Expected improvement:**
- Current: 11/138 passing (7%)
- After Phase 1: ~80-100/138 passing (58-72%)
- Failures will be due to star imports or other unrelated issues

## Follow-up Work

After Phase 1 validation:

1. **Analyze remaining failures** - categorize by issue type:
   - Star imports needed
   - Type arguments
   - Inner classes
   - Other issues

2. **Implement Phase 2 (if needed)** - based on failure analysis:
   - If many star import failures → implement Option B.1
   - If few star import failures → defer to later iteration

3. **Update documentation**:
   - `ITERATION_RESULTS.md` with Phase 1 findings
   - `IMPLEMENTATION_PLAN.md` if architecture changes
