# Resolve Callback Approach: Clean Architecture for Star Import Resolution

## Overview

Instead of exposing import lists through interfaces, we add a **resolution callback mechanism** to `JavaClassifierType`. This provides a clean separation of concerns:

- **JavaModel** knows **how to resolve** (has import context)
- **FIR** knows **what to try** (has symbol provider)

## Interface Design

### Core Interfaces

```kotlin
// core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt

interface JavaClassifierType : JavaType {
    val classifier: JavaClassifier?
    val classifierQualifiedName: String
    
    /**
     * Whether this type is already resolved.
     * 
     * - `true`: `classifier` is available OR `classifierQualifiedName` is fully qualified
     * - `false`: Type needs resolution via star imports or other context
     * 
     * For PSI and javac-wrapper: always returns `true`
     * For java-direct: returns `false` for simple names not found in single-type imports
     */
    val isResolved: Boolean
        get() = true  // Default: already resolved
    
    /**
     * Attempts to resolve this type by trying candidate package names.
     * 
     * Called by FIR when `isResolved == false` to resolve simple type names
     * using import context (star imports, java.lang, etc.)
     * 
     * @param tryResolve Lambda that takes a fully qualified name and returns
     *                   true if the type exists, false otherwise
     * @return The fully qualified name if resolved, null if unresolved/ambiguous
     * 
     * Implementation should:
     * 1. Try candidates in Java resolution order (java.lang, then star imports)
     * 2. Return first successful match
     * 3. Return null if no match or multiple matches (ambiguous)
     * 
     * For PSI/javac-wrapper: Never called (isResolved is always true)
     * For java-direct: Implements star import resolution logic
     */
    fun resolve(tryResolve: (String) -> Boolean): String? = null
}
```

### Why This Design is Better

#### Comparison with Previous Approaches

| Aspect | Expose Imports List | Expose Resolved Imports | Resolve Callback |
|--------|-------------------|------------------------|------------------|
| **Separation of Concerns** | ❌ FIR knows Java import rules | ❌ FIR knows Java import rules | ✅ JavaModel knows rules |
| **Data Exposure** | ❌ Exposes internal lists | ❌ Exposes FIR objects | ✅ No data exposure |
| **Ambiguity Detection** | ⚠️ FIR must implement | ⚠️ FIR must implement | ✅ JavaModel implements |
| **Lazy Evaluation** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Breaking Changes** | ⚠️ Interface addition | ⚠️ Interface addition | ✅ Default impl, no-op |
| **PSI/javac Impact** | ✅ None (default empty) | ✅ None (default empty) | ✅ None (never called) |

## Implementation

### 1. java-direct Implementation

```kotlin
// compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/ast/types/JavaClassifierTypeOverAst.kt

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
        
        // Fully qualified in source
        if (typeName.contains('.')) return typeName
        
        // Check single-type imports (highest precedence after local types)
        imports?.simpleImports?.get(typeName)?.asString()?.let { return it }
        
        // Not resolved - need star import resolution
        return typeName  // Return simple name
    }
    
    override val isResolved: Boolean
        get() {
            // Resolved if:
            // 1. Local classifier found, OR
            // 2. Fully qualified name (contains '.'), OR  
            // 3. Found in single-type imports
            val typeName = node.text
            return classifier != null 
                || typeName.contains('.')
                || imports?.simpleImports?.containsKey(typeName) == true
        }
    
    override fun resolve(tryResolve: (String) -> Boolean): String? {
        // Only called when isResolved == false (simple name not in single-type imports)
        val simpleName = node.text
        
        // Java resolution order per JLS
        
        // 1. Try java.lang.* (implicit import, highest precedence for star imports)
        val javaLangFqn = "java.lang.$simpleName"
        if (tryResolve(javaLangFqn)) {
            return javaLangFqn
        }
        
        // 2. Try explicit star imports in declaration order
        val starImports = imports?.starImports ?: emptyList()
        var foundFqn: String? = null
        
        for (packageFqName in starImports) {
            val candidateFqn = "${packageFqName.asString()}.$simpleName"
            if (tryResolve(candidateFqn)) {
                if (foundFqn != null) {
                    // Found in multiple packages - ambiguous per JLS §7.5.2
                    // Return null to signal ambiguity
                    return null
                }
                foundFqn = candidateFqn
            }
        }
        
        return foundFqn  // null if not found in any star import
    }
}
```

### 2. FIR Type Conversion

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
            // ... existing code for resolved JavaClass ...
        }
        
        is JavaTypeParameter -> {
            // ... existing code for type parameters ...
        }
        
        null -> {
            val qualifiedName = this.classifierQualifiedName
            
            val classId = if (!isResolved && !qualifiedName.contains('.')) {
                // Unresolved simple name - use resolution callback
                resolveSimpleName(qualifiedName, this, session)
            } else {
                // Already resolved or fully qualified
                ClassId.topLevel(FqName(qualifiedName))
            }
            
            classId.constructClassLikeType(emptyArray(), isMarkedNullable = lowerBound != null, attributes)
        }
        
        else -> ConeErrorType(ConeSimpleDiagnostic("Unexpected classifier: $classifier", DiagnosticKind.Java))
    }
}

private fun resolveSimpleName(
    simpleName: String,
    javaType: JavaClassifierType,
    session: FirSession
): ClassId {
    // Ask JavaModel to resolve using FIR's symbol provider as validator
    val resolvedFqn = javaType.resolve { candidateFqn ->
        val classId = ClassId.topLevel(FqName(candidateFqn))
        session.symbolProvider.getClassLikeSymbolByClassId(classId) != null
    }
    
    return when {
        resolvedFqn != null -> {
            // Successfully resolved to a unique type
            ClassId.topLevel(FqName(resolvedFqn))
        }
        
        javaType.resolve { true } == null && hasMultipleCandidates(javaType) -> {
            // Ambiguous - found in multiple star imports
            // TODO: Report proper diagnostic with JLS reference
            ConeErrorType(ConeSimpleDiagnostic(
                "Type $simpleName is ambiguous (JLS §7.5.2)", 
                DiagnosticKind.Java
            )).lookupTag as? ConeClassLikeLookupTag
                ?: ClassId.topLevel(FqName(simpleName))
        }
        
        else -> {
            // Not found - fall back to root package (current behavior)
            ClassId.topLevel(FqName(simpleName))
        }
    }
}

private fun hasMultipleCandidates(javaType: JavaClassifierType): Boolean {
    // Check if resolve found multiple candidates by trying with always-true predicate
    var count = 0
    javaType.resolve { 
        count++
        count <= 1  // Stop after finding 2
    }
    return count > 1
}
```

## Advantages in Detail

### 1. Correct Ambiguity Detection

The callback approach naturally handles JLS §7.5.2 ambiguity rules:

```kotlin
override fun resolve(tryResolve: (String) -> Boolean): String? {
    var foundFqn: String? = null
    
    for (packageFqName in starImports) {
        val candidateFqn = "${packageFqName.asString()}.$simpleName"
        if (tryResolve(candidateFqn)) {
            if (foundFqn != null) {
                // Second match - ambiguous!
                return null
            }
            foundFqn = candidateFqn
        }
    }
    
    return foundFqn
}
```

### 2. Follows Java Resolution Order

The implementation in `JavaClassifierTypeOverAst.resolve()` directly implements JLS resolution order:

1. ✅ Local types (handled by `classifier`)
2. ✅ Single-type imports (handled by `classifierQualifiedName`)
3. ✅ Current package types (handled by `classifier` via `localScope`)
4. ✅ `java.lang.*` (handled in `resolve()`)
5. ✅ Explicit star imports (handled in `resolve()`)

### 3. No Duplication or Leakage

- Imports stay private in `JavaClassifierTypeOverAst`
- No need to expose import lists via interfaces
- No need to pass imports through FirJavaClass
- PSI/javac-wrapper completely unaffected

### 4. Testable in Isolation

```kotlin
// Easy to test java-direct resolution logic
val javaType = JavaClassifierTypeOverAst(...)
assertFalse(javaType.isResolved)

val resolved = javaType.resolve { fqn ->
    fqn == "java.util.List"  // Simulate symbol provider
}

assertEquals("java.util.List", resolved)
```

### 5. Extensible

Future enhancements (like handling inner classes, static imports) can be added to `resolve()` without changing interfaces:

```kotlin
override fun resolve(tryResolve: (String) -> Boolean): String? {
    // ... existing code ...
    
    // Future: Try nested classes in star-imported types
    for (packageFqName in starImports) {
        val outerClassFqn = "${packageFqName.asString()}.$outerClassName"
        val nestedFqn = "$outerClassFqn.$simpleName"
        if (tryResolve(nestedFqn)) {
            // ...
        }
    }
}
```

## Alternative: Simplified Ambiguity Handling

If detecting ambiguity in the callback is too complex, we can simplify:

```kotlin
/**
 * Returns ALL candidate fully qualified names for this unresolved type.
 * FIR will validate which ones exist and detect ambiguity.
 */
fun resolveCandidates(): List<String> {
    val simpleName = node.text
    val candidates = mutableListOf<String>()
    
    // java.lang.*
    candidates.add("java.lang.$simpleName")
    
    // Explicit star imports
    for (pkg in imports?.starImports ?: emptyList()) {
        candidates.add("${pkg.asString()}.$simpleName")
    }
    
    return candidates
}
```

Then FIR validates and detects ambiguity:

```kotlin
val candidates = javaType.resolveCandidates()
val validCandidates = candidates.filter { candidateFqn ->
    val classId = ClassId.topLevel(FqName(candidateFqn))
    session.symbolProvider.getClassLikeSymbolByClassId(classId) != null
}

when (validCandidates.size) {
    0 -> // Not found
    1 -> // Resolved uniquely
    else -> // Ambiguous
}
```

**Trade-off:**
- Simpler JavaModel implementation (just returns candidates)
- FIR does ambiguity detection (but needs to know Java rules)
- More allocations (creates list of candidates)

**Recommendation:** Use the callback approach - it's cleaner and more correct.

## Files to Modify

### 1. Interface Definition

**File:** `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt`

Add to `JavaClassifierType`:
```kotlin
val isResolved: Boolean get() = true
fun resolve(tryResolve: (String) -> Boolean): String? = null
```

### 2. java-direct Implementation  

**File:** `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/model/ast/types/JavaClassifierTypeOverAst.kt`

Implement:
- `override val isResolved: Boolean`
- `override fun resolve(tryResolve: (String) -> Boolean): String?`

### 3. FIR Type Conversion

**File:** `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`

Modify `classifier == null` branch to:
- Check `isResolved`
- Call `resolve()` if unresolved
- Handle ambiguity

## Comparison with Previous Solutions

### Previous Approach (Expose Imports)

```kotlin
interface JavaClassifierType {
    val starImportPackages: List<FqName>  // ❌ Exposes internal data
}

// FIR must implement Java resolution rules
null -> {
    for (pkg in this.starImportPackages) {
        // ❌ FIR knows about Java import semantics
    }
}
```

**Problems:**
- FIR needs to know Java resolution order
- Duplicates imports in every type object
- Violates encapsulation

### Callback Approach (Proposed)

```kotlin
interface JavaClassifierType {
    val isResolved: Boolean
    fun resolve(tryResolve: (String) -> Boolean): String?
}

// JavaModel implements Java resolution rules
override fun resolve(tryResolve: ...) {
    // ✅ Java resolution logic stays in JavaModel
}

// FIR just validates candidates
null -> {
    javaType.resolve { fqn ->
        session.symbolProvider.getClassLikeSymbolByClassId(
            ClassId.topLevel(FqName(fqn))
        ) != null
    }
}
```

**Benefits:**
- Clean separation: JavaModel knows "how", FIR knows "what"
- No data exposure
- Correct encapsulation
- Natural ambiguity handling

## Migration Path

### Phase 1: Add Interface Members with Defaults

Add to `JavaClassifierType` with default implementations:
- `val isResolved: Boolean get() = true`
- `fun resolve(...): String? = null`

**Impact:** Zero - all existing implementations work unchanged

### Phase 2: Implement in java-direct

Override in `JavaClassifierTypeOverAst`:
- Implement `isResolved` check
- Implement `resolve()` with star import logic

**Impact:** java-direct types can now resolve via star imports

### Phase 3: Use in FIR

Modify `JavaTypeConversion.kt` to check `isResolved` and call `resolve()`

**Impact:** Star imports work for java-direct, PSI/javac-wrapper unaffected

### Phase 4: Testing

Run tests and iterate on ambiguity detection, error messages

## Conclusion

The resolve callback approach is **architecturally superior** because:

1. ✅ **Clean separation of concerns** - JavaModel knows resolution rules
2. ✅ **No data leakage** - Imports stay private
3. ✅ **Correct ambiguity handling** - Natural implementation of JLS rules
4. ✅ **No breaking changes** - Default implementations work
5. ✅ **Testable** - Can test JavaModel and FIR independently
6. ✅ **Extensible** - Easy to add more resolution logic

This is the **recommended approach** for implementing star import resolution.
