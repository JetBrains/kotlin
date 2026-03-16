# Cross-File Ambiguity Detection: Minimal Solution

## Problem Statement

**Test**: `testInheritanceAmbiguity2`

**Scenario**:
```java
// a/x.java
package a;
public class x {
    public class Z {}
}

// a/i.java
package a;
public interface i {
    public class Z {}
}

// a/i2.java
package a;
public interface i2 extends i {}

// a/y.java
package a;
public class y extends x implements i2 {
    public Z getZ() { return null; }  // javac: reference to Z is ambiguous
}
```

**Current behavior**: java-direct resolves `Z` to one of the classes (arbitrarily), missing the ambiguity.

**Expected behavior**: Detect ambiguity and report `MISSING_DEPENDENCY_CLASS` error (matching javac and PSI-based FIR).

**Why it fails**: 
- `y.java` is parsed independently, creating its own `JavaResolutionContext`
- `localClassProvider` only knows about classes in `y.java`
- When resolving supertypes (`x`, `i2`), `supertype.classifier` returns `null` for cross-file classes
- Cannot navigate to `i2`'s supertypes to find `i.Z`
- Only finds `x.Z`, doesn't detect ambiguity with `i.Z`

---

## Current Architecture Limitation

### File-Scoped Resolution (Current)

Each `.java` file is parsed on-demand with isolated resolution context:

```kotlin
// JavaClassFinderOverAstImpl.parseTopLevelClassFromFile()
val resolutionContext = JavaResolutionContext.create(root)  // file-scoped
val node = root.getChildrenByType("CLASS").firstOrNull { ... }
return JavaClassOverAst(node, resolutionContext, outerClass = null)
```

**Problem**: `JavaResolutionContext.localClassProvider` is a closure over the file's AST:
```kotlin
val localClassProvider: (Name) -> JavaClass? = { name ->
    findClassNode(root, name)?.let { classNode ->
        JavaClassOverAst(classNode, contextRef!!, outerClass = null)
    }
}
```

This means:
- `y`'s context can only see classes declared in `y.java`
- When `y extends x implements i2`, both `x` and `i2` are in different files
- `supertype.classifier` returns `null` → cannot navigate supertype hierarchy
- Ambiguity detection stops at direct supertypes visible in same file

### Why Supertypes Are Not Resolved

In `JavaResolutionContext.findInnerClassFromSupertypes()`:

```kotlin
for (supertype in javaClass.supertypes) {
    val supertypeRef = supertype.presentableText.substringBefore('<')...
    val supertypeClass = localClassProvider(Name.identifier(supertypeRef)) ?: continue
    // ^^^ returns null for x and i2 (not in y.java)
}
```

And in `JavaResolutionContext.resolveFromSupertypes()`:

```kotlin
val supertypeClass = supertype.classifier as? JavaClass
if (supertypeClass != null) {
    // Never executed for cross-file supertypes
    resolveFromSupertypesRecursive(...)
}
```

---

## Proposed Solution: Minimal Eager Supertype Chain Parsing

### Key Insight

We don't need full cross-file visibility. We only need to:
1. **Parse supertype declarations** when resolving a class's members
2. **Cache parsed supertype chains** to avoid redundant parsing
3. **Navigate supertype hierarchy** to collect all inner classes with same name

### What Needs To Be Parsed Eagerly

**Minimal data from supertype files**:
- Class name (already indexed)
- Supertype references (`extends X implements Y`) - need to parse
- Inner class names (already extractable from AST)
- Package name (already indexed)

**What does NOT need to be parsed**:
- Method bodies
- Field initializers
- Full type signatures
- Annotations (except when needed for type resolution)
- Anything not required for supertype chain traversal

### Implementation Strategy

#### 1. Add Supertype Chain Resolver to JavaClassFinderOverAstImpl

```kotlin
class JavaClassFinderOverAstImpl(...) {
    
    // Cache: ClassId -> list of supertype ClassIds (direct only)
    private val supertypeCache: MutableMap<ClassId, List<ClassId>> = ConcurrentHashMap()
    
    /**
     * Parses just enough of a class to extract its direct supertypes.
     * This is lightweight - only reads extends/implements clauses.
     */
    fun getDirectSupertypes(classId: ClassId): List<ClassId> {
        return supertypeCache.getOrPut(classId) {
            val files = findFilesForClass(classId) // uses existing index
            if (files.isEmpty()) return@getOrPut emptyList()
            
            val file = files.first() // assume single declaration
            val source = tryReadFile(file.path) ?: return@getOrPut emptyList()
            val builder = parseJavaToSyntaxTreeBuilder(source, 0)
            val root = buildSyntaxTree(builder, source)
            
            // Extract package for resolution
            val packageFqName = extractPackageName(root)
            
            // Find the target class node
            val classNode = findClassInTree(root, classId) ?: return@getOrPut emptyList()
            
            // Extract supertype references (just the names)
            val supertypes = mutableListOf<ClassId>()
            
            classNode.findChildByType("EXTENDS_LIST")
                ?.getChildrenByType("JAVA_CODE_REFERENCE")
                ?.forEach { ref ->
                    resolveSupertypeReference(ref.text, packageFqName)?.let {
                        supertypes.add(it)
                    }
                }
            
            classNode.findChildByType("IMPLEMENTS_LIST")
                ?.getChildrenByType("JAVA_CODE_REFERENCE")
                ?.forEach { ref ->
                    resolveSupertypeReference(ref.text, packageFqName)?.let {
                        supertypes.add(it)
                    }
                }
            
            supertypes
        }
    }
    
    /**
     * Resolve a supertype reference like "x" or "i2" to a ClassId.
     * Uses same-package resolution for simple names.
     */
    private fun resolveSupertypeReference(ref: String, packageFqName: FqName): ClassId? {
        val simpleName = ref.substringBefore('<').trim()
        
        // Try same package first
        if (!simpleName.contains('.')) {
            val samePackageId = ClassId(packageFqName, Name.identifier(simpleName))
            if (index[packageFqName]?.containsKey(simpleName) == true) {
                return samePackageId
            }
        }
        
        // Handle fully qualified or imported names
        // (can be enhanced with import parsing if needed)
        return null
    }
    
    /**
     * Recursively collects all inner class names from the supertype hierarchy.
     * Returns Map<simpleName, Set<ClassId>> to detect ambiguities.
     */
    fun collectInheritedInnerClasses(classId: ClassId): Map<String, Set<ClassId>> {
        val result = mutableMapOf<String, MutableSet<ClassId>>()
        val visited = mutableSetOf<ClassId>()
        
        fun collectRecursive(current: ClassId) {
            if (current in visited) return
            visited.add(current)
            
            // Get inner classes of current class
            val innerClasses = getInnerClassNames(current)
            for (innerName in innerClasses) {
                val innerClassId = ClassId(
                    current.packageFqName,
                    current.relativeClassName.child(Name.identifier(innerName))
                )
                result.getOrPut(innerName) { mutableSetOf() }.add(innerClassId)
            }
            
            // Recurse into supertypes
            for (supertypeId in getDirectSupertypes(current)) {
                collectRecursive(supertypeId)
            }
        }
        
        collectRecursive(classId)
        return result
    }
    
    /**
     * Gets inner class names from index (lightweight, no full parsing).
     */
    private fun getInnerClassNames(classId: ClassId): Set<String> {
        // This requires enhancing the index to track inner classes
        // OR doing minimal parsing similar to getDirectSupertypes
        // Decision: parse minimally to extract inner class names
        
        val files = findFilesForClass(classId)
        if (files.isEmpty()) return emptySet()
        
        val file = files.first()
        val source = tryReadFile(file.path) ?: return emptySet()
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        
        val classNode = findClassInTree(root, classId) ?: return emptySet()
        
        return classNode.children
            .filter { it.type.toString() == "CLASS" }
            .mapNotNull { it.findChildByType("IDENTIFIER")?.text }
            .toSet()
    }
}
```

#### 2. Modify JavaResolutionContext to Use ClassFinder

**Problem**: `JavaResolutionContext` currently doesn't have access to `JavaClassFinder`.

**Solution**: Add optional `classFinderProvider` parameter:

```kotlin
class JavaResolutionContext private constructor(
    val packageFqName: FqName,
    private val simpleImports: Map<String, FqName>,
    private val starImports: List<FqName>,
    private val localClassProvider: (Name) -> JavaClass?,
    private val typeParametersInScope: Map<String, JavaTypeParameter> = emptyMap(),
    private val containingClassProvider: (() -> JavaClass?)? = null,
    private val classFinderProvider: (() -> JavaClassFinderOverAstImpl)? = null,  // NEW
) {
    
    /**
     * Enhanced version that checks cross-file supertypes for ambiguity.
     */
    private fun findInnerClassFromSupertypes(
        name: Name, 
        javaClass: JavaClass, 
        visited: MutableSet<JavaClass>
    ): JavaClass? {
        if (javaClass in visited) return null
        visited.add(javaClass)
        
        val allFound = mutableSetOf<JavaClass>()
        
        // Try to get the ClassId of the containing class
        val containingClassId = (javaClass as? JavaClassOverAst)?.let { cls ->
            ClassId(cls.resolutionContext.packageFqName, cls.name)
        }
        
        // If we have a class finder, use it to detect cross-file ambiguities
        if (containingClassId != null) {
            val classFinder = classFinderProvider?.invoke()
            if (classFinder != null) {
                val inheritedInners = classFinder.collectInheritedInnerClasses(containingClassId)
                val candidates = inheritedInners[name.asString()] ?: emptySet()
                
                if (candidates.size > 1) {
                    // Ambiguity detected across multiple supertypes
                    return null
                }
                
                // If exactly one candidate, resolve it
                if (candidates.size == 1) {
                    val candidateId = candidates.first()
                    return classFinder.findClass(JavaClassFinder.Request(candidateId))
                }
            }
        }
        
        // Fall back to local resolution (same-file supertypes)
        for (supertype in javaClass.supertypes) {
            val supertypeRef = supertype.presentableText.substringBefore('<').trim()
                .substringBefore('.').trim()
            
            if (supertypeRef.isEmpty()) continue
            
            val supertypeClass = localClassProvider(Name.identifier(supertypeRef)) ?: continue
            
            supertypeClass.findInnerClass(name)?.let { found ->
                allFound.add(found)
            }
            
            findInnerClassFromSupertypes(name, supertypeClass, visited)?.let { found ->
                allFound.add(found)
            }
        }
        
        if (allFound.size > 1) return null
        return allFound.firstOrNull()
    }
}
```

#### 3. Wire ClassFinder to Resolution Context

Modify `JavaClassFinderOverAstImpl.parseTopLevelClassFromFile()`:

```kotlin
private fun parseTopLevelClassFromFile(path: Path, simpleName: String): JavaClassOverAst? {
    val source = tryReadFile(path) ?: return null
    val builder = parseJavaToSyntaxTreeBuilder(source, 0)
    val root = buildSyntaxTree(builder, source)
    val resolutionContext = JavaResolutionContext.create(root, classFinderProvider = { this })
    //                                                           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    val node = root.getChildrenByType("CLASS").firstOrNull { ... } ?: return null
    return JavaClassOverAst(node, resolutionContext, outerClass = null)
}
```

And update `JavaResolutionContext.create()`:

```kotlin
companion object {
    fun create(
        root: JavaSyntaxNode,
        classFinderProvider: (() -> JavaClassFinderOverAstImpl)? = null  // NEW
    ): JavaResolutionContext {
        // ... existing code ...
        
        return JavaResolutionContext(
            packageFqName = packageFqName,
            simpleImports = simpleImports,
            starImports = starImports,
            localClassProvider = localClassProvider,
            classFinderProvider = classFinderProvider  // NEW
        ).also { contextRef = it }
    }
}
```

---

## Implementation Phases

### Phase 1: Infrastructure (No Behavioral Change)

1. Add `classFinderProvider` parameter to `JavaResolutionContext`
2. Add `getDirectSupertypes()` to `JavaClassFinderOverAstImpl` (returns empty list initially)
3. Wire up the provider in `parseTopLevelClassFromFile()`
4. Verify no test regressions

### Phase 2: Supertype Parsing (Minimal)

1. Implement `getDirectSupertypes()` to parse extends/implements clauses
2. Implement `resolveSupertypeReference()` for same-package resolution
3. Add unit tests for supertype chain extraction
4. Verify parsing is lazy and cached

### Phase 3: Inner Class Collection

1. Implement `getInnerClassNames()` (minimal parsing of inner CLASS nodes)
2. Implement `collectInheritedInnerClasses()` with cycle detection
3. Add unit tests for inherited inner class collection

### Phase 4: Ambiguity Detection

1. Update `findInnerClassFromSupertypes()` to use `collectInheritedInnerClasses()`
2. Update `resolveFromSupertypes()` similarly
3. Test `testInheritanceAmbiguity2` - should now pass
4. Run full test suite for regressions

---

## Performance Considerations

### Parsing Cost

**Before**: Each class parsed once on-demand (current behavior).

**After**: Each class parsed once + lightweight re-parse for supertype extraction.

**Lightweight re-parse**:
- Parse AST (already fast with KMP parser)
- Extract extends/implements text (no full type resolution)
- Extract inner class names (simple node iteration)
- **No method parsing, no field parsing, no annotation parsing**

**Caching**: `supertypeCache` ensures each class's supertypes are extracted at most once.

**Benchmarking**: If this shows measurable slowdown, we can:
1. Enhance the initial index to include supertype names
2. Add inner class names to initial index
3. Both would eliminate the need for re-parsing

### Memory Cost

**Additional caches**:
- `supertypeCache: Map<ClassId, List<ClassId>>` - ~100 bytes per class
- For 10K classes: ~1MB
- Acceptable for compilation session

### Index Enhancement Alternative (Future Optimization)

Instead of re-parsing for supertypes, enhance `buildIndex()`:

```kotlin
private fun tryBuildFileEntry(path: Path): FileEntry? {
    // ... existing code ...
    
    // NEW: also extract supertype names and inner class structure
    val supertypeRefs = extractSupertypeRefs(root)
    val innerClassTree = extractInnerClassTree(root)
    
    return FileEntry(path, packageFqName, classNames, supertypeRefs, innerClassTree)
}
```

This would make supertype chain traversal O(1) instead of requiring re-parsing, but adds complexity to indexing.

---

## Edge Cases

### 1. Generic Supertypes

```java
class y<T> extends x<T> implements i2<T> { ... }
```

**Solution**: `presentableText.substringBefore('<')` already handles this - we only need the raw class name.

### 2. Qualified Supertype References

```java
class y extends some.other.pkg.x { ... }
```

**Solution**: `resolveSupertypeReference()` can be enhanced to handle qualified names by looking up in the global index.

### 3. Imported Supertypes

```java
import other.pkg.x;
class y extends x { ... }
```

**Solution**: Parse imports in `getDirectSupertypes()` similar to `JavaResolutionContext.create()`.

### 4. Circular Inheritance

```java
// Invalid Java, but parser might not reject it
class A extends B { }
class B extends A { }
```

**Solution**: `visited` set in `collectInheritedInnerClasses()` prevents infinite loops.

### 5. Binary Supertypes (JDK classes)

```java
class y extends ArrayList implements i2 { ... }
```

**Solution**: `getDirectSupertypes()` returns empty list for `ArrayList` (not in source index). Binary supertypes are handled by existing FIR callback resolution, which works correctly.

---

## Testing Strategy

### Unit Tests

1. `getDirectSupertypes()` with various syntaxes
2. `collectInheritedInnerClasses()` with diamond inheritance
3. Circular reference handling
4. Ambiguity detection with 2+ supertypes having same inner class

### Integration Tests

1. `testInheritanceAmbiguity2` - should pass
2. All other inheritance tests - should not regress
3. Performance benchmark on large codebases (if available)

---

## Alternative Approaches Considered

### Alternative 1: Full Cross-File Visibility

**Approach**: Make `localClassProvider` global, allowing resolution of any class in any file.

**Rejected because**:
- Requires parsing all files upfront (defeats on-demand parsing)
- Higher memory cost (all ASTs in memory)
- Complexity in managing shared resolution contexts

### Alternative 2: FIR-Level Ambiguity Detection

**Approach**: Let FIR detect ambiguity when resolving `Z` in `getZ()` return type.

**Rejected because**:
- FIR resolution is based on single-class lookups via `JavaClassFinder`
- FIR doesn't have visibility into "all classes named Z in supertype hierarchy"
- Would require major FIR architecture changes

### Alternative 3: Accept the Limitation

**Approach**: Document cross-file ambiguity as a known limitation.

**Rejected because**:
- The limitation is solvable with minimal cost
- javac detects this error, so java-direct should too
- Only requires lightweight supertype chain traversal

---

## Conclusion

**The proposed solution is minimal** because:
1. ✅ Only parses supertype chains on-demand (when resolving members)
2. ✅ Only parses minimal data (extends/implements clauses, inner class names)
3. ✅ Caches results to avoid redundant work
4. ✅ Doesn't change overall architecture (still file-scoped, on-demand)
5. ✅ Solves the specific problem (cross-file ambiguity) without over-engineering

**Estimated implementation time**: 4-6 hours for all phases + testing.

**Risk level**: Low - changes are isolated to `JavaClassFinderOverAstImpl` and `JavaResolutionContext`, with clear rollback points after each phase.
