# Type Resolution Design: Star Imports and java.lang

## Problem Statement

The java-direct implementation currently fails to resolve simple type names that depend on star imports or the automatic `java.lang.*` import:

**Current Status:**
- ✅ **Fully qualified names:** `java.util.ArrayList` → works
- ✅ **Single-type imports:** `import java.util.ArrayList;` → works (handled in Java Model)
- ❌ **java.lang automatic import:** `Object` → fails (returns `classifierQualifiedName = "Object"`, FIR looks in root package)
- ❌ **Star imports:** `import java.util.*;` then `List` → fails (not resolved to `java.util.List`)

**Impact:** Only 11/138 (7%) box tests pass. Tests using simple names for `java.lang` types or star-imported types fail.

**Root Cause:** 
1. Java Model returns simple names like `"Object"` when no single-type import exists
2. FIR's fallback path in `JavaTypeConversion.kt` does `ClassId.topLevel(FqName("Object"))` → looks in root package
3. Neither Java Model nor FIR handles star imports or automatic `java.lang.*` import

## Java Import Resolution Rules (JLS Summary)

Per the Java Language Specification (Chapter 7, Packages and Modules):

### Resolution Order

When resolving a simple type name, Java searches in this order:

1. **Types in current compilation unit** (top-level or nested)
2. **Single-type imports** (`import java.util.List;`)
3. **Types in current package**
4. **Star imports** (`import java.util.*;` and implicit `import java.lang.*;`)

### Key Rules

**Rule 1:** Single-type import of duplicate names → **compile error** (unless same type)
```java
import java.util.List;
import java.awt.List;  // ERROR: duplicate import
```

**Rule 2:** Single-type imports **shadow** star imports
```java
import java.util.*;
import java.awt.List;  // Shadows java.util.List from star import
```

**Rule 3:** Star imports **never shadow anything** (JLS §7.5.2)
- If a name exists in multiple star imports → **ambiguous** (compile error when used)
- Star imports don't shadow each other or single-type imports

**Rule 4:** Every Java file has implicit `import java.lang.*;` (JLS §7.5.5)

## Architectural Approaches Considered

### Approach 1: Expose Star Import Lists

Add property to `JavaClassifierType`:
```kotlin
interface JavaClassifierType {
    val starImportPackages: List<FqName>
}
```

**Issues:** 
- FIR must implement Java resolution rules (wrong layer)
- Duplicates import data in every type object
- Violates encapsulation

### Approach 2: Pass Imports as Parameter

Thread imports through type conversion call chain:
```kotlin
fun toConeKotlinType(..., starImports: List<FqName>)
```

**Issues:**
- Requires updating ~20+ call sites
- Invasive change
- Still requires FIR to know Java resolution order

### Approach 3: Resolve Callback (RECOMMENDED)

Add resolution callback to `JavaClassifierType`:
```kotlin
interface JavaClassifierType {
    val isResolved: Boolean
    fun resolve(tryResolve: (String) -> Boolean): String?
}
```

**Advantages:**
- ✅ **Separation of concerns:** JavaModel implements Java rules, FIR validates existence
- ✅ **No data exposure:** Imports stay private in java-direct
- ✅ **Natural ambiguity detection:** Callback can detect multiple matches
- ✅ **Zero impact on PSI/javac-wrapper:** Default implementations, never called
- ✅ **Testable in isolation:** Can test JavaModel resolution without FIR

## Recommended Solution: Resolve Callback Approach

### Interface Design

```kotlin
// core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt

interface JavaClassifierType : JavaType {
    val classifier: JavaClassifier?
    val classifierQualifiedName: String
    
    /**
     * Whether this type is already resolved.
     * 
     * - `true`: classifier is available OR classifierQualifiedName is fully qualified
     * - `false`: Type needs resolution via star imports
     * 
     * Default: `true` (PSI/javac-wrapper are always resolved)
     */
    val isResolved: Boolean
        get() = true
    
    /**
     * Resolves unresolved simple type names using import context.
     * 
     * Called by FIR when isResolved == false to resolve via star imports.
     * 
     * @param tryResolve Lambda that validates if a fully qualified name exists
     * @return Resolved FQN, or null if not found/ambiguous
     * 
     * Implementation should:
     * 1. Try java.lang.* first (implicit import)
     * 2. Try explicit star imports in order
     * 3. Return first match, or null if no match or ambiguous
     * 
     * Default: null (never called for PSI/javac-wrapper)
     */
    fun resolve(tryResolve: (String) -> Boolean): String? = null
}
```

### Implementation in java-direct

```kotlin
class JavaClassifierTypeOverAst(
    private val node: AstJavaType,
    private val localScope: LocalJavaScope?,
    private val imports: AstJavaImports?,
    typeParameters: List<JavaTypeParameter>
) : JavaClassifierType {
    
    override val classifier: JavaClassifier? by lazy {
        val simpleName = node.text
        localScope?.findClass(Name.identifier(simpleName))
    }
    
    override val classifierQualifiedName: String by lazy {
        val typeName = node.text
        if (typeName.contains('.')) return typeName
        imports?.simpleImports?.get(typeName)?.asString() ?: typeName
    }
    
    override val isResolved: Boolean
        get() {
            val typeName = node.text
            return classifier != null 
                || typeName.contains('.')
                || imports?.simpleImports?.containsKey(typeName) == true
        }
    
    override fun resolve(tryResolve: (String) -> Boolean): String? {
        val simpleName = node.text
        
        // 1. Try java.lang.* (implicit import per JLS §7.5.5)
        val javaLangFqn = "java.lang.$simpleName"
        if (tryResolve(javaLangFqn)) {
            return javaLangFqn
        }
        
        // 2. Try explicit star imports (JLS §7.5.2)
        val starImports = imports?.starImports ?: emptyList()
        var foundFqn: String? = null
        
        for (packageFqName in starImports) {
            val candidateFqn = "${packageFqName.asString()}.$simpleName"
            if (tryResolve(candidateFqn)) {
                if (foundFqn != null) {
                    // Ambiguous per JLS §7.5.2
                    return null
                }
                foundFqn = candidateFqn
            }
        }
        
        return foundFqn
    }
}
```

### Implementation in FIR

```kotlin
// compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt

private fun JavaClassifierType.toConeKotlinTypeForFlexibleBound(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode,
    attributes: ConeAttributes,
    source: KtSourceElement?,
    lowerBound: ConeLookupTagBasedType? = null
): ConeLookupTagBasedType {
    return when (val classifier = classifier) {
        is JavaClass -> {
            // ... existing code ...
        }
        
        is JavaTypeParameter -> {
            // ... existing code ...
        }
        
        null -> {
            val qualifiedName = this.classifierQualifiedName
            
            val classId = if (!isResolved && !qualifiedName.contains('.')) {
                // Unresolved simple name - delegate to Java Model
                resolveSimpleName(qualifiedName, this, session, source)
            } else {
                // Already resolved or fully qualified
                ClassId.topLevel(FqName(qualifiedName))
            }
            
            classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
        }
        
        else -> ConeErrorType(...)
    }
}

private fun resolveSimpleName(
    simpleName: String,
    javaType: JavaClassifierType,
    session: FirSession,
    source: KtSourceElement?
): ClassId {
    // Ask JavaModel to resolve using FIR's symbol provider as validator
    val resolvedFqn = javaType.resolve { candidateFqn ->
        val classId = ClassId.topLevel(FqName(candidateFqn))
        session.symbolProvider.getClassLikeSymbolByClassId(classId) != null
    }
    
    return when {
        resolvedFqn != null -> {
            // Successfully resolved
            ClassId.topLevel(FqName(resolvedFqn))
        }
        
        else -> {
            // Not found or ambiguous
            // TODO: Distinguish between "not found" and "ambiguous" for better diagnostics
            ClassId.topLevel(FqName(simpleName))  // Fall back to root package
        }
    }
}
```

## Why This Approach is Superior

### 1. Correct Separation of Concerns

| Layer | Responsibility |
|-------|---------------|
| **JavaModel (java-direct)** | Knows Java resolution rules (JLS), implements resolution logic |
| **FIR** | Knows what types exist (symbol provider), validates candidates |

### 2. No Data Exposure

- Imports remain private in `JavaClassifierTypeOverAst`
- No need to add properties to `JavaClass` or `JavaClassifierType` to expose import lists
- Clean encapsulation

### 3. Natural Ambiguity Handling

The callback pattern naturally detects ambiguous imports:

```kotlin
var foundFqn: String? = null
for (pkg in starImports) {
    if (tryResolve("$pkg.$name")) {
        if (foundFqn != null) return null  // Ambiguous!
        foundFqn = "$pkg.$name"
    }
}
```

### 4. Zero Impact on Existing Implementations

**PSI/javac-wrapper:** 
- `isResolved` always returns `true` (default)
- `resolve()` never called
- No behavior change

**java-direct:**
- Implements `isResolved` and `resolve()`
- Gains star import resolution capability

### 5. Testable in Isolation

```kotlin
val javaType = JavaClassifierTypeOverAst(...)
assertFalse(javaType.isResolved)

val resolved = javaType.resolve { fqn -> 
    fqn == "java.util.List"  // Mock symbol provider
}

assertEquals("java.util.List", resolved)
```

## Files to Modify

### 1. Interface Definition

**File:** `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt`

**Changes:**
- Add `val isResolved: Boolean get() = true` to `JavaClassifierType`
- Add `fun resolve(tryResolve: (String) -> Boolean): String? = null` to `JavaClassifierType`

### 2. java-direct Implementation

**File:** `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/ast/types/JavaClassifierTypeOverAst.kt`

**Changes:**
- Override `isResolved` to check if type is local, fully qualified, or in single-type imports
- Override `resolve()` to implement Java resolution logic with star imports

### 3. FIR Type Conversion

**File:** `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`

**Changes:**
- Modify `classifier == null` branch in `toConeKotlinTypeForFlexibleBound()`
- Check `isResolved`, call `resolve()` if needed
- Add helper function `resolveSimpleName()`

## Expected Results

**Test Improvement:** From 11/138 (7%) to ~120-130/138 (87-94%) passing

**Types Now Resolved:**
- `Object`, `String`, `Integer`, etc. (java.lang types)
- Types from explicit star imports: `import java.util.*;` → `List` resolves

**Remaining Failures:** Likely due to:
- Type arguments (generics)
- Inner classes
- Static imports
- Other unrelated issues

## Migration Path

### Phase 1: Add Interface Members
- Add `isResolved` and `resolve()` to `JavaClassifierType` with defaults
- **Impact:** Zero - all existing implementations work unchanged

### Phase 2: Implement in java-direct
- Override `isResolved` check
- Implement `resolve()` with star import logic
- **Impact:** java-direct types can now resolve via star imports

### Phase 3: Use in FIR
- Modify `JavaTypeConversion.kt` to check `isResolved` and call `resolve()`
- **Impact:** Star imports work, PSI/javac-wrapper unaffected

### Phase 4: Testing and Iteration
- Run box tests
- Analyze remaining failures
- Add diagnostics for ambiguous imports
- Refine error messages

## Future Enhancements

The callback approach is extensible for future needs:

1. **Static imports:** Can be added to `resolve()` logic
2. **Inner classes from star imports:** Can check nested types
3. **Better diagnostics:** Can distinguish "not found" vs "ambiguous"
4. **Performance optimization:** Can add caching in Java Model

All enhancements can be added without changing interfaces.
