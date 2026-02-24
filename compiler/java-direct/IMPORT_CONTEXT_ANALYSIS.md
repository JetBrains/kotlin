# Import Context Analysis: Where to Store Star Imports

## Problem

Star imports are a **file-level** concept in Java, not a type-level concept. Currently, the proposed solution (Option B.1 in STAR_IMPORTS_SOLUTION.md) suggests adding `getStarImportPackages()` to `JavaClassifierType`, which is architecturally incorrect because:

1. **JavaClassifierType represents a type reference**, not a file or class declaration
2. **Multiple type references in the same file would duplicate import information**
3. **It violates the single responsibility principle** - type references shouldn't know about file-level imports

## Investigation Findings

### Current Architecture in java-direct

**File-level imports are stored in `JavaClassOverAst`:**

```kotlin
// compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt:16-22
class JavaClassOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    override val outerClass: JavaClass? = null,
    private val localScope: LocalJavaScope? = null,
    private val imports: JavaImports = JavaImports.EMPTY  // ← IMPORTS ARE HERE
) : JavaElementOverAst(node, source), JavaClass
```

**Imports are passed to types when they're created:**

```kotlin
// compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt:73-82
override val supertypes: Collection<JavaClassifierType>
    get() {
        val result = mutableListOf<JavaClassifierType>()
        node.findChildByType("EXTENDS_LIST")?.getChildrenByType("JAVA_CODE_REFERENCE")?.forEach {
            result.add(JavaClassifierTypeOverAst(it, source, localScope, imports))  // ← PASSED HERE
        }
        node.findChildByType("IMPLEMENTS_LIST")?.getChildrenByType("JAVA_CODE_REFERENCE")?.forEach {
            result.add(JavaClassifierTypeOverAst(it, source, localScope, imports))  // ← AND HERE
        }
        return result
    }
```

### JavaClass Interface

**Location:** `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaElements.kt`

```kotlin
interface JavaClass : JavaClassifier, JavaTypeParameterListOwner, JavaModifierListOwner {
    val fqName: FqName?
    val supertypes: Collection<JavaClassifierType>
    val innerClassNames: Collection<Name>
    fun findInnerClass(name: Name): JavaClass?
    val outerClass: JavaClass?
    val isInterface: Boolean
    val isAnnotationType: Boolean
    val isEnum: Boolean
    val isRecord: Boolean
    val isSealed: Boolean
    val permittedTypes: Sequence<JavaClassifierType>
    val lightClassOriginKind: LightClassOriginKind?
    val methods: Collection<JavaMethod>
    val fields: Collection<JavaField>
    val constructors: Collection<JavaConstructor>
    val recordComponents: Collection<JavaRecordComponent>
    fun hasDefaultConstructor(): Boolean
}
```

**Key observation:** JavaClass interface does NOT have any file-level information like imports.

### FirJavaClass Has Access to JavaClass

**Location:** `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/FirJavaFacade.kt:142-143`

```kotlin
private fun createFirJavaClass(
    javaClass: JavaClass,
    classSymbol: FirRegularClassSymbol,
    parentClassSymbol: FirRegularClassSymbol?,
    classId: ClassId,
    classJavaTypeParameterStack: MutableJavaTypeParameterStack,
): FirJavaClass {
    return buildJavaClass {
        this.javaClass = javaClass  // ← FirJavaClass stores reference to JavaClass
        // ...
    }
}
```

### Type Conversion Context

Type conversion happens in `JavaTypeConversion.kt` with this signature:

```kotlin
private fun JavaClassifierType.toConeKotlinTypeForFlexibleBound(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode,
    attributes: ConeAttributes,
    source: KtSourceElement?,
    lowerBound: ConeLookupTagBasedType? = null
): ConeLookupTagBasedType
```

**Problem:** At this point, we only have `JavaClassifierType` (the type reference), not `JavaClass` (the class declaration).

## Architectural Options

### Option 1: Add Star Imports to JavaClass Interface ✅ RECOMMENDED

**Rationale:** Imports are conceptually part of the class's **compilation context**, similar to how the package is part of the class context.

**Changes:**

1. **Extend JavaClass interface:**

```kotlin
// core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaElements.kt
interface JavaClass : JavaClassifier, JavaTypeParameterListOwner, JavaModifierListOwner {
    val fqName: FqName?
    // ... existing members ...
    
    // NEW: File-level import context
    val starImportPackages: List<FqName>
        get() = emptyList()  // Default implementation for backward compatibility
}
```

2. **Implement in java-direct:**

```kotlin
// compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt
class JavaClassOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    override val outerClass: JavaClass? = null,
    private val localScope: LocalJavaScope? = null,
    private val imports: JavaImports = JavaImports.EMPTY
) : JavaElementOverAst(node, source), JavaClass {
    // ... existing code ...
    
    override val starImportPackages: List<FqName>
        get() = imports.starImports
}
```

3. **Pass imports through JavaClassifierType:**

Since `JavaClassifierType` already receives imports in java-direct, we need a way to access them during type conversion. Two sub-options:

**Option 1a: Add `containingClass` to JavaClassifierType**

```kotlin
// core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt
interface JavaClassifierType : JavaType {
    val classifier: JavaClassifier?
    val classifierQualifiedName: String
    
    // NEW: Access to containing class for file-level context
    val containingClass: JavaClass?
        get() = null  // Default for backward compatibility
}
```

**Option 1b: Store star imports in JavaClassifierType** (similar to original B.1, but better justified)

```kotlin
interface JavaClassifierType : JavaType {
    val classifier: JavaClassifier?
    val classifierQualifiedName: String
    
    // NEW: Star imports from the file (cached from containing class)
    val starImportPackages: List<FqName>
        get() = emptyList()
}
```

**Comparison:**

| Aspect | 1a: containingClass | 1b: starImportPackages |
|--------|---------------------|------------------------|
| **Purity** | More pure - provides access to context | Less pure - caches file-level data in type |
| **Convenience** | Need to access `containingClass?.starImportPackages` | Direct access |
| **Performance** | Extra indirection | Cached value |
| **Use cases** | Could enable other file-level queries | Single-purpose |

### Option 2: Pass Imports via Type Conversion Context

**Rationale:** Add imports as a parameter to type conversion functions.

**Changes:**

```kotlin
private fun JavaClassifierType.toConeKotlinTypeForFlexibleBound(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    mode: FirJavaTypeConversionMode,
    attributes: ConeAttributes,
    source: KtSourceElement?,
    lowerBound: ConeLookupTagBasedType? = null,
    starImportPackages: List<FqName> = emptyList()  // NEW parameter
): ConeLookupTagBasedType
```

**Problems:**
- Requires threading imports through all call sites
- Type conversion is called from many places - requires tracing all call paths
- More invasive change (~20+ call sites to update)

### Option 3: Store Imports in FirSession or Context

**Rationale:** Make star imports available as contextual information during type conversion.

**Problems:**
- FirSession is shared across files - can't store per-file data there
- Would need a thread-local or context receiver pattern
- Overcomplicated for this use case

## Recommendation

### Implement Option 1b: Add `starImportPackages` to Both JavaClass and JavaClassifierType

**Why this is the best solution:**

1. **Architecturally correct at class level:** `JavaClass.starImportPackages` makes sense - imports are part of the class's compilation context
2. **Pragmatic at type level:** `JavaClassifierType.starImportPackages` caches the value for convenient access during type conversion
3. **Minimal changes:** Only two interface additions with default implementations
4. **No breaking changes:** Default implementations maintain backward compatibility
5. **Follows existing patterns:** Similar to how `JavaClassifierType` has both `classifier` (for local resolution) and `classifierQualifiedName` (for FIR resolution)

### Implementation Plan

**Phase 1: Add to Interfaces**

1. Add `starImportPackages: List<FqName>` to `JavaClass` interface with default empty list
2. Add `starImportPackages: List<FqName>` to `JavaClassifierType` interface with default empty list

**Phase 2: Implement in java-direct**

1. Implement `JavaClassOverAst.starImportPackages` to return `imports.starImports`
2. Implement `JavaClassifierTypeOverAst.starImportPackages` to return the imports passed in constructor
   - Already available via `private val imports: JavaImports?` field

**Phase 3: Use in FIR Type Conversion**

1. Modify `JavaTypeConversion.kt` in the `classifier == null` path:

```kotlin
null -> {
    val qualifiedName = this.classifierQualifiedName
    val classId = if (!qualifiedName.contains('.')) {
        resolveSimpleNameWithStarImports(
            qualifiedName,
            this.starImportPackages,  // ← USE NEW PROPERTY
            session
        )
    } else {
        ClassId.topLevel(FqName(qualifiedName))
    }
    classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
}

private fun resolveSimpleNameWithStarImports(
    simpleName: String,
    starImportPackages: List<FqName>,
    session: FirSession
): ClassId {
    val name = Name.identifier(simpleName)
    
    // 1. Check java.lang.* (automatic import per JLS §7.5.5)
    val javaLangId = ClassId(FqName("java.lang"), name)
    if (session.symbolProvider.getClassLikeSymbolByClassId(javaLangId) != null) {
        return javaLangId
    }
    
    // 2. Check explicit star imports
    for (packageFqName in starImportPackages) {
        val classId = ClassId(packageFqName, name)
        if (session.symbolProvider.getClassLikeSymbolByClassId(classId) != null) {
            return classId
        }
    }
    
    // 3. Fall back to root package
    return ClassId.topLevel(FqName(simpleName))
}
```

### Files to Modify

1. **`core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaElements.kt`**
   - Add `val starImportPackages: List<FqName> get() = emptyList()` to `JavaClass` interface

2. **`core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt`**
   - Find and add `val starImportPackages: List<FqName> get() = emptyList()` to `JavaClassifierType` interface

3. **`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt`**
   - Override `starImportPackages` property

4. **`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/ast/types/JavaClassifierTypeOverAst.kt`**
   - Override `starImportPackages` property

5. **`compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`**
   - Add `resolveSimpleNameWithStarImports()` helper function
   - Modify `classifier == null` path to use it

### Impact on Other Implementations

**PSI (`JavaClassImpl`):** Default implementation returns `emptyList()` - no impact

**javac-wrapper (`TreeBasedClass`):** Default implementation returns `emptyList()` - no impact

**Binary classes (`BinaryJavaClass`):** Default implementation returns `emptyList()` - no impact (binary classes don't have source-level imports)

All existing implementations continue to work without modification. Only java-direct needs to implement the property.

## Java Import Resolution Rules (JLS)

Understanding Java's import resolution rules is critical for correct implementation. These rules are defined in the Java Language Specification (JLS), primarily in Chapter 7 (Packages and Modules) and Chapter 6 (Names).

### Import Types (JLS §7.5)

1. **Single-type-import declaration** (§7.5.1): `import java.util.List;`
   - Imports a specific type by its canonical name
   - Has highest precedence in resolution

2. **Type-import-on-demand declaration** (§7.5.2): `import java.util.*;`
   - Imports all accessible types from a package as needed
   - Has lower precedence than single-type imports
   - **Never causes any other declaration to be shadowed** (key rule!)

3. **Automatic java.lang import** (§7.5.5):
   - Every compilation unit implicitly has `import java.lang.*;`
   - Behaves as a type-import-on-demand declaration
   - Treated as if it appears immediately after any package declaration

### Resolution Order and Precedence

When resolving a simple type name (like `List`), the Java compiler searches in this order:

1. **Types declared in the current compilation unit** (top-level or nested)
2. **Types imported by single-type-import declarations** (explicit imports)
3. **Types declared in the current package**
4. **Types imported by type-import-on-demand declarations** (star imports)
   - Includes explicit `import pkg.*;` declarations
   - Includes implicit `import java.lang.*;`

**Key principle (JLS §6.5):** A variable will be chosen in preference to a type, and a type will be chosen in preference to a package.

### Conflict and Ambiguity Rules

#### Rule 1: Duplicate Single-Type Imports (JLS §7.5.1)

**Compile-time error** if two single-type-import declarations import different types with the same simple name:

```java
import java.util.List;         // java.util.List
import java.awt.List;          // java.awt.List - COMPILE ERROR!
```

**Exception:** The duplicate declaration is ignored if both imports reference the **exact same type**:

```java
import java.util.List;
import java.util.List;         // Duplicate but allowed - same type
```

#### Rule 2: Single-Type Import vs. Current Package Type (JLS §7.5.1)

**Compile-time error** if a single-type-import imports a type whose simple name is `n`, and the compilation unit also declares a top-level type named `n`:

```java
package com.example;
import java.util.List;         // COMPILE ERROR!

public class List {            // Same name as imported type
}
```

#### Rule 3: On-Demand Import Ambiguity (JLS §7.5.2)

If a simple name could resolve to types in **multiple on-demand imports**, it's **ambiguous** and causes a compile-time error **only when the name is actually used**:

```java
import java.util.*;            // Has List class
import java.awt.*;             // Also has List class

// No error yet - imports are valid

List list = ...;               // COMPILE ERROR: List is ambiguous
```

**Resolution:** Use a single-type-import to resolve ambiguity:

```java
import java.util.*;
import java.awt.*;
import java.util.List;         // Explicitly choose util.List

List list = ...;               // OK - resolves to java.util.List
```

#### Rule 4: Single-Type Import Shadows On-Demand Imports (JLS §7.5.1, §7.5.2)

A single-type-import **shadows** types that would otherwise be imported by on-demand declarations:

```java
import java.util.*;            // Would import java.util.List
import java.awt.List;          // Shadows java.util.List

List list = ...;               // OK - resolves to java.awt.List (single-type import wins)
```

This applies even to `java.lang.*`:

```java
import java.lang.String;       // Redundant but allowed - shadows implicit java.lang.*
```

#### Rule 5: Type-Import-On-Demand Never Shadows (JLS §7.5.2)

**Critical rule:** A type-import-on-demand declaration **never causes any other declaration to be shadowed**.

This means:
- Star imports don't shadow each other
- Star imports don't shadow single-type imports
- Star imports don't shadow types in the current package
- `java.lang.*` doesn't shadow explicit imports

### Practical Resolution Algorithm

For a simple name `N`, the resolution algorithm is:

```
1. Is N a type in the current compilation unit?
   YES → Use it (shadowing all imports)
   NO → Continue to step 2

2. Is N imported by a single-type-import?
   YES → Use it (shadowing on-demand imports)
   NO → Continue to step 3

3. Is N a type in the current package?
   YES → Use it (shadowing on-demand imports)
   NO → Continue to step 4

4. Is N imported by exactly ONE type-import-on-demand declaration?
   YES → Use it
   NO → Continue to step 5

5. Is N imported by MULTIPLE type-import-on-demand declarations?
   YES → COMPILE ERROR: N is ambiguous
   NO → Continue to step 6

6. COMPILE ERROR: Cannot resolve symbol N
```

### Implementation Implications

For the java-direct implementation, we need to:

1. **Store both single-type imports AND star imports** separately
   - Single-type imports already handled in Java Model (`imports.simpleImports`)
   - Star imports need to be passed to FIR (`imports.starImports`)

2. **Resolution in Java Model** (already implemented):
   - Check local classes (current file)
   - Check single-type imports
   - Return simple name if not found → hand off to FIR

3. **Resolution in FIR** (to be implemented):
   - When `classifier == null` and name is simple:
     - Check `java.lang.*` first (implicit import)
     - Check explicit star imports in order
     - If found in exactly one package → resolve
     - If found in multiple packages → ERROR (ambiguous)
     - If not found → fall back to root package (current behavior)

4. **Error handling for ambiguity**:
   - FIR should detect when a simple name matches types in multiple star-imported packages
   - Report compile error with appropriate diagnostic

### Order of Star Imports

The JLS does not specify a particular order for checking on-demand imports - if a name is found in multiple on-demand imports, it's simply ambiguous. However, for implementation:

1. Check `java.lang.*` first (automatic import)
2. Check explicit star imports in **declaration order**
3. Report ambiguity if found in multiple packages

**Rationale for checking java.lang first:** While the JLS treats all on-demand imports equally in terms of ambiguity, checking `java.lang.*` first provides better error messages (if `String` is ambiguous, we want to report the explicit import as conflicting with `java.lang.String`, not vice versa).

## Verification

After implementation:

1. Run box tests - expect improvement from 11/138 (7%) to ~120-130/138 (87-94%)
2. Verify PSI and javac-wrapper tests still pass (should be unaffected)
3. Check that binary class loading still works (should be unaffected)
4. Test ambiguity detection with multiple star imports
5. Test single-type import shadowing of star imports

## Alternative: Minimal Approach

If adding to JavaClass is controversial, we can do **Option 1b only** (add just to JavaClassifierType), and have java-direct pass imports directly. This is less architecturally pure but more pragmatic and requires no changes to JavaClass interface.
