# FirSession Resolution Integration Analysis

## Document Purpose

This document analyzes how type resolution should access FirSession in the java-direct module, evaluating different architectural approaches for integrating external type resolution (via FIR symbol providers) into the Java Model layer.

**Status**: Analysis Complete  
**Last Updated**: 2026-02-23

---

## The Problem

The java-direct module needs to resolve Java type references (e.g., `extends Base`, `List<String>`) to their actual class definitions. This resolution requires access to:

1. **Local scope**: Classes defined in the same Java file (already implemented via `LocalJavaScope`)
2. **External classes**: Classes from other files, packages, standard library, and dependencies

External class resolution requires access to `FirSession.symbolProvider`, but there's a **critical architectural challenge**:

- ❌ `JavaClassFinder` is created **before** `FirSession` exists
- ❌ `JavaClass` instances are created during `JavaClassFinder` operations
- ❌ `FirSession` is only available **later** when `FirJavaFacade` is created
- ❌ The Java Model layer is designed to be FIR-independent

---

## How Type Resolution Currently Works in FIR

### Call Chain Analysis

```
FirJavaClass.superTypeRefs (lazy)
  └─> computeSuperTypeRefsByJavaClass()
      └─> javaClass.supertypes  // JavaClass.supertypes getter
          └─> Returns Collection<JavaClassifierType>
              └─> Each JavaClassifierType has:
                  - classifier: JavaClassifier? (JavaClass or JavaTypeParameter)
                  - classifierQualifiedName: String
                  - typeArguments: List<JavaType?>

FirJavaFacade.convertJavaClassToFir()
  └─> Creates FirJavaClass
      └─> Wraps JavaClass in FirJavaClass
          └─> FirJavaClass.superTypeRefs is lazy
              └─> Calls javaClass.supertypes
                  └─> Each JavaClassifierType is converted to FirTypeRef
                      via JavaType.toFirJavaTypeRef()
                          └─> Creates FirJavaTypeRef with JavaType reference
                              └─> Later resolved via toFirResolvedTypeRef()
```

### Critical Discovery: Resolution Happens in FIR Layer!

**Key insight from `JavaTypeConversion.kt`**:

```kotlin
internal fun JavaType.toFirJavaTypeRef(session: FirSession, source: KtSourceElement?): FirJavaTypeRef = 
    buildJavaTypeRef {
        type = this@toFirJavaTypeRef  // Just wraps the JavaType!
        this.source = source
    }

// Later, during type enhancement:
fun FirTypeRef.resolveIfJavaType(
    session: FirSession, 
    javaTypeParameterStack: JavaTypeParameterStack,
    source: KtSourceElement?,
    mode: FirJavaTypeConversionMode = FirJavaTypeConversionMode.DEFAULT
): FirTypeRef = when (this) {
    is FirJavaTypeRef -> type.toFirResolvedTypeRef(session, javaTypeParameterStack, source, mode)
    // ...
}
```

The resolution happens in two phases:

1. **Java Model Phase** (java-direct): Returns `JavaClassifierType` with:
   - `classifierQualifiedName: String` (e.g., "Base", "java.util.List")
   - `classifier: JavaClassifier?` (CAN BE NULL!)

2. **FIR Conversion Phase** (FirJavaFacade): 
   - Wraps JavaType in `FirJavaTypeRef`
   - Later resolves via `toConeTypeProjection()` using `session` and `javaTypeParameterStack`
   - Uses `JavaClassifierType.classifier` if available, otherwise uses `classifierQualifiedName`

**Critical code from `JavaTypeConversion.kt:191-247`**:

```kotlin
private fun JavaClassifierType.toConeKotlinTypeForFlexibleBound(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    // ...
): ConeLookupTagBasedType {
    return when (val classifier = classifier) {
        is JavaClass -> {
            // Use JavaClass.classId to create ClassId
            var classId = classifier.classId!!
            // Resolve via session.symbolProvider
        }
        is JavaTypeParameter -> {
            // Resolve via javaTypeParameterStack
            val symbol = javaTypeParameterStack[classifier]
        }
        null -> {
            // FALLBACK: Parse classifierQualifiedName as FqName
            val classId = ClassId.topLevel(FqName(this.classifierQualifiedName))
            classId.constructClassLikeType(...)
        }
    }
}
```

**This means**: `JavaClassifierType.classifier` can be `null` and FIR will handle it!

---

## Proposed Solutions

### Solution 1: No Resolution in Java Model (RECOMMENDED ✅)

**Approach**: Keep Java Model completely resolution-free.

**Implementation**:
- `JavaClassifierType.classifier` returns `null` for external types
- `JavaClassifierType.classifierQualifiedName` returns the type name as string
- `LocalJavaScope` provides local resolution only
- FIR layer handles all external resolution using `classifierQualifiedName`

**Advantages**:
- ✅ **Zero architectural changes** - works with existing FIR infrastructure
- ✅ **No circular dependencies** - Java Model stays FIR-independent
- ✅ **Proven pattern** - FIR already handles `classifier == null` case
- ✅ **Simple implementation** - already 90% done in Iteration 1
- ✅ **No session threading needed** - FirSession lives only in FIR layer

**Disadvantages**:
- ⚠️ `JavaClassifierType.classifier` will be `null` for most types (only locals resolved)
- ⚠️ Must ensure `classifierQualifiedName` is correct (fully qualified)

**Required Changes**:
1. Fix `JavaClassifierTypeOverAst.classifierQualifiedName` to resolve qualified names correctly
2. Implement import tracking to construct full FQNs
3. Keep `classifier` returning local scope results or `null`

**Code Example**:
```kotlin
class JavaClassifierTypeOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    private val localScope: LocalJavaScope?,
    private val imports: JavaImports?
) : JavaTypeOverAst(node, source), JavaClassifierType {
    
    // Returns local classes only, null for external
    override val classifier: JavaClassifier? by lazy {
        val simpleName = node.text
        localScope?.findClass(Name.identifier(simpleName))
    }
    
    // Returns fully qualified name (critical for FIR resolution)
    override val classifierQualifiedName: String by lazy {
        val simpleName = node.text
        
        // Check if it's a simple name that needs qualification
        if (!simpleName.contains('.')) {
            // Try simple imports first
            imports?.simpleImports?.get(simpleName)?.asString()
                ?: simpleName  // Keep simple name, FIR will check star imports
        } else {
            simpleName  // Already qualified
        }
    }
    
    override val isRaw: Boolean get() = typeArguments.isEmpty() && !isPrimitive()
    override val typeArguments: List<JavaType> get() = emptyList() // TODO: parse
    override val presentableText: String get() = node.text
}
```

---

### Solution 2: Thread FirSession Through Java Model (NOT RECOMMENDED ❌)

**Approach**: Make FirSession available to Java Model.

**Option 2a: Wrapper Around JavaClassFinder**

```kotlin
class SessionAwareJavaClassFinder(
    private val delegate: JavaClassFinder,
    private val session: FirSession
) : JavaClassFinder {
    override fun findClass(request: Request): JavaClass? {
        val javaClass = delegate.findClass(request)
        // Inject session somehow?
        return javaClass?.let { SessionAwareJavaClass(it, session) }
    }
}

class SessionAwareJavaClass(
    private val delegate: JavaClass,
    private val session: FirSession
) : JavaClass by delegate {
    override val supertypes: Collection<JavaClassifierType> 
        get() = delegate.supertypes.map { 
            SessionAwareJavaClassifierType(it, session) 
        }
}

class SessionAwareJavaClassifierType(
    private val delegate: JavaClassifierType,
    private val session: FirSession
) : JavaClassifierType by delegate {
    override val classifier: JavaClassifier? by lazy {
        delegate.classifier ?: resolveViaFir(session, classifierQualifiedName)
    }
}
```

**Disadvantages**:
- ❌ **Complex wrapper hierarchy** - wraps every Java Model class
- ❌ **Session lifecycle issues** - FirSession doesn't exist when JavaClassFinder is created
- ❌ **Violates architecture** - Java Model should be FIR-independent
- ❌ **High maintenance** - need to wrap all Java Model interfaces
- ❌ **Circular dependency risk** - Java Model → FIR → Java Model

**Option 2b: Store Session in JavaClassOverAst**

```kotlin
class JavaClassOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    private var session: FirSession? = null  // Injected later
) : JavaClass {
    fun setSession(session: FirSession) {
        this.session = session
    }
}
```

**Disadvantages**:
- ❌ **Mutable state** - violates immutability principle
- ❌ **Thread safety issues** - need synchronization
- ❌ **Lifecycle complexity** - when to inject session?
- ❌ **Partial initialization** - objects invalid until session set

---

### Solution 3: Move Resolution to FirJavaClass (INTERESTING BUT COMPLEX ⚠️)

**Approach**: Override resolution methods in FirJavaClass to bypass JavaClass.

**Implementation**:
```kotlin
class FirJavaClass {
    override val superTypeRefs: List<FirTypeRef> by lazy {
        val superTypesRefs = nonEnhancedSuperTypes.ifEmpty {
            computeSuperTypeRefsByJavaClass()
        }
        // ...
    }
    
    private fun computeSuperTypeRefsByJavaClass(): List<FirTypeRef> {
        // OPTION: Parse supertypes directly from JavaClass
        // and resolve using session HERE
        val javaSupertypes = javaClass?.supertypes ?: return emptyList()
        
        return javaSupertypes.map { javaType ->
            // Resolve immediately using session
            resolveJavaType(javaType, session, javaTypeParameterStack)
        }
    }
    
    private fun resolveJavaType(
        javaType: JavaClassifierType, 
        session: FirSession,
        stack: JavaTypeParameterStack
    ): FirTypeRef {
        // Custom resolution logic here
        // Can access session.symbolProvider
        val classId = parseClassId(javaType.classifierQualifiedName)
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId)
        // Build FirTypeRef
    }
}
```

**Advantages**:
- ✅ **Session readily available** - FirJavaClass has session
- ✅ **No Java Model changes** - resolution happens in FIR layer
- ✅ **Full control** - can customize resolution logic

**Disadvantages**:
- ⚠️ **Duplicates FIR logic** - `JavaTypeConversion.kt` already does this
- ⚠️ **Maintenance burden** - need to keep in sync with FIR changes
- ⚠️ **Misses enhancements** - FIR's enhancement logic may not apply
- ⚠️ **Type parameter handling** - need to reimplement stack management

---

## Recommendation

**Use Solution 1: No Resolution in Java Model** ✅

### Rationale

1. **Already works**: FIR's `JavaTypeConversion.kt` handles `classifier == null` explicitly
2. **Minimal changes**: Just need correct `classifierQualifiedName` implementation
3. **Clean architecture**: Java Model stays FIR-independent as designed
4. **Low risk**: No circular dependencies or lifecycle issues
5. **Proven**: Existing PSI-based implementation likely works similarly

### Implementation Strategy for Iterations 2-4

**Iteration 2: Improve classifierQualifiedName**
- Extract package name from file context
- Handle simple type references (e.g., "Base" → keep as "Base")
- Handle qualified references (e.g., "java.util.List" → "java.util.List")
- Do NOT attempt resolution - just get the name right

**Iteration 3: Import Tracking**
- Parse import statements into `JavaImports` data class
- Use imports to qualify simple names when possible:
  - `import java.util.ArrayList; ... ArrayList` → `classifierQualifiedName = "java.util.ArrayList"`
- Pass `JavaImports` to `JavaClassifierTypeOverAst`

**Iteration 4: Let FIR Handle the Rest**
- FIR's `JavaTypeConversion.kt` will:
  - Check `classifier` first (local classes will be found)
  - Fall back to `classifierQualifiedName` for external classes
  - Use `session.symbolProvider` to resolve
  - Handle star imports, java.lang package, etc.

---

## Critical Code Paths to Understand

### Where Resolution Actually Happens

1. **`JavaTypeConversion.kt:191-247`**: Main resolution logic
   - Input: `JavaClassifierType` with `classifier` or `classifierQualifiedName`
   - Output: `ConeLookupTagBasedType` (resolved FIR type)
   - Uses: `session.symbolProvider.getClassLikeSymbolByClassId()`

2. **`FirJavaClass.kt:131-141`**: Supertype computation
   - Calls `javaClass.supertypes` (Java Model layer)
   - Converts each to `FirJavaTypeRef` (just wraps it)
   - Resolution happens lazily during type enhancement

3. **`FirJavaFacade.kt:57-92`**: Session access point
   - Has `session: FirSession` field
   - All Java → FIR conversion passes through here
   - This is where session is available

### What Java Model Must Provide

```kotlin
interface JavaClassifierType : JavaType {
    val classifier: JavaClassifier?          // Can be null! FIR handles it
    val classifierQualifiedName: String      // MUST be correct (FQ if possible)
    val typeArguments: List<JavaType?>       // For generics
    val isRaw: Boolean                       // For raw types
}
```

**Requirements**:
- ✅ `classifier`: Return local classes if found, otherwise `null`
- ✅ `classifierQualifiedName`: Return fully qualified name if knowable, otherwise simple name
- ✅ `typeArguments`: Parse from AST (Iteration 6)
- ✅ `isRaw`: Check if type has arguments in source

---

## Testing Strategy

### Unit Tests for classifierQualifiedName

```kotlin
@Test
fun testSimpleName() {
    val source = "class Derived extends Base {}"
    // classifierQualifiedName should be "Base"
    // classifier should check localScope
}

@Test
fun testQualifiedName() {
    val source = "class MyClass extends java.util.ArrayList {}"
    // classifierQualifiedName should be "java.util.ArrayList"
    // classifier should be null (not in localScope)
}

@Test
fun testImportedName() {
    val source = """
        import java.util.ArrayList;
        class MyClass extends ArrayList {}
    """
    // classifierQualifiedName should be "java.util.ArrayList" (via import resolution)
    // classifier should be null
}
```

### Integration Tests (Box Tests)

Let FIR handle resolution and verify box tests pass:
- Simple inheritance from local classes
- Inheritance from standard library (java.lang.Object)
- Inheritance from imported classes
- Generic types (List<String>)

---

## Open Questions & Decisions

### Q1: Should we attempt FQN resolution in Java Model?

**Answer**: YES, but only for simple cases:
- If type name is already qualified (contains '.'): use as-is
- If we have imports: qualify via simple imports map
- Otherwise: return simple name, let FIR handle it

### Q2: What about star imports (import java.util.*)?

**Answer**: Store star imports in `JavaImports`, but don't attempt resolution in Java Model. FIR will handle:
- Check simple imports first
- Check current package
- Check star imports against `session.symbolProvider`
- Check java.lang package

### Q3: How to handle inner classes (Outer.Inner)?

**Answer**: 
- Parse as qualified name "Outer.Inner"
- FIR will resolve Outer first, then find Inner nested class
- Java Model just needs to preserve the qualified structure

### Q4: Type parameters (<T extends Comparable<T>>)?

**Answer**: Iteration 6 will handle:
- Extract type parameters from AST
- Store bounds as `JavaClassifierType`
- FIR will resolve bounds using `javaTypeParameterStack`

---

## Summary for Agents

When implementing Iterations 2-4, follow this approach:

1. **DO**: Make `classifierQualifiedName` return correct names (FQ when possible)
2. **DO**: Implement import tracking and use for name qualification
3. **DO**: Keep `classifier` returning local classes or `null`
4. **DO**: Trust FIR to handle external resolution

5. **DON'T**: Try to access `FirSession` from Java Model
6. **DON'T**: Implement custom resolution logic in Java Model
7. **DON'T**: Create session-aware wrappers
8. **DON'T**: Worry about star imports or java.lang - FIR handles it

**Key principle**: Java Model provides names and structure, FIR provides resolution.

---

## Document Change Log

- 2026-02-23: Initial analysis based on FIR codebase investigation
