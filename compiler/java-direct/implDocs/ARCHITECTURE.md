# Java-Direct: Architecture & Reference Guide

This document contains architecture decisions, callback patterns, key files, and reference
implementation mappings. Consult on-demand when implementing new features — not required reading
for every iteration.

---

## Key Architecture Decisions

### 1. Type Resolution in FIR Layer (Not Java Model)
Java Model provides names (`classifierQualifiedName`), FIR resolves them via `session.symbolProvider`. **No `FirSession` access in Java Model**.

### 2. Callback Pattern for Resolution

**CRITICAL**: Always prefer callback-based resolution over hardcoded lists.

`resolve(tryResolve: (ClassId) -> Boolean): ClassId?` in `JavaClassifierType` allows Java Model to implement Java resolution rules (imports, scope, JLS priority) while FIR validates existence via the callback.

**Key insight (iter 43)**: Resolution returns `ClassId` directly, not a string. This avoids the ambiguity where `"a.b"` could mean either `ClassId("a","b")` (package a, class b) or `ClassId("","a.b")` (nested class a.b). See `implDocs/RESOLUTION_PIPELINE.md`.

**Established callback patterns** (use these as templates for new features):

| Feature | Interface Method | FIR Callback |
|---------|-----------------|--------------|
| Type resolution | `JavaClassifierType.resolve(tryResolve: (ClassId) -> Boolean, getSupertypeClassIds: ((ClassId) -> List<ClassId>)? = null): ClassId?` | `symbolProvider.getClassLikeSymbolByClassId` |
| Annotation resolution | `JavaAnnotation.resolveAnnotation(tryResolve: (ClassId) -> Boolean): ClassId?` | `symbolProvider.getClassLikeSymbolByClassId` |
| Enum class resolution | `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve)` | `findClassId()` in `JavaTypeConversion.kt` |
| TYPE_USE filtering | `JavaType.filterTypeUseAnnotations(isTypeUse)` | `isTypeUseAnnotationClass()` |
| Constant evaluation | `JavaField.resolveInitializerValue(resolveReference)` | `resolveExternalFieldValue()` in `FirJavaFacade.kt` |

The second `getSupertypeClassIds` parameter (added later) lets java-direct walk
already-resolved supertype chains transitively when resolving inherited inner classes
(e.g., `Derived → Base → Map → Entry`) without re-entering full FIR resolution.

**Why callbacks**: Allows java-direct to handle its own resolution without affecting PSI-based or javac-wrapper implementations.

### 3. Hybrid Class Finder
`CombinedJavaClassFinder` tries source class finder first, falls back to binary class finder for JDK/library classes.

### 4. Resolution Context Pattern
`JavaResolutionContext` encapsulates all resolution data (package, imports, type parameters, containing class). Passed through AST nodes. Use `withTypeParameters()`, `withContainingClass()` to extend scope.

### 5. Two-Phase Type Parameter Construction
When type parameters can reference each other in bounds (e.g., `<E, S extends Element<E>>`):
1. Create all instances first with basic context
2. Update context with all siblings via `updateResolutionContext()`

### 6. PSI/Java-Direct Discrimination
When shared FIR code needs different behavior for java-direct:
```kotlin
// Java-direct classes have null source (no PSI)
val isJavaDirectClass = classSource == null && origin.fromSource
```

### 7. Implicit Supertypes
Java classes have implicit inheritance:
- Enums -> `java.lang.Enum<E>`
- Annotation types -> `java.lang.annotation.Annotation`
- Classes without extends -> `java.lang.Object`

---

## Key Files

### Model layer (over AST)
| File | Purpose |
|------|---------|
| `JavaClassOverAst.kt` | Java class model, `memberResolutionContext`, `innerClassCache` |
| `JavaTypeOverAst.kt` | Type representations, `classifierQualifiedName`, wildcards, type parameters |
| `JavaMemberOverAst.kt` | Methods, fields, value parameters |
| `JavaAnnotationOverAst.kt` | Annotation parsing and resolution |
| `JavaPackageOverAst.kt` | Package model |
| `JavaRecordComponentOverAst.kt` | Record component model |
| `JavaElementOverAst.kt` | Common base for `*OverAst` classes |

### Parsing & AST
| File | Purpose |
|------|---------|
| `parse.kt` | KMP parser invocation |
| `JavaLightTree.kt` | Flat-array AST (`JavaLightNode` value class), `childrenCache` memoizes child lists |
| `JavaSourceFileReader.kt` | VFS-backed file reads |

### Indexing & class finding
| File | Purpose |
|------|---------|
| `JavaClassFinderOverAstImpl.kt` | Source class finder, on-demand lookups over a built index |
| `JavaSourceIndex.kt` | Source-tree indexing (files per package, class names per file) |
| `CombinedJavaClassFinder.kt` | Source-first, binary-fallback hybrid |
| `JavaDirectComponentRegistrar.kt` | Plugin registration, hybrid finder wiring |

### Resolution
| File | Purpose |
|------|---------|
| `JavaResolutionContext.kt` | Import/type-parameter scope management, `resolve()` entry point |
| `JavaScopeResolver.kt` | Local-class / type-parameter scope lookup (`findLocalClassCache`) |
| `JavaImportResolver.kt` | Single-type + on-demand import handling, JLS priority |
| `JavaInheritedMemberResolver.kt` | Inherited-inner-class resolution, aggregated-inner handling |
| `JavaSupertypeGraph.kt` | Cross-file supertype graph, `supertypeCache`, `inheritedInnerClassesCache` |

### Utilities
| File | Purpose |
|------|---------|
| `CacheHelpers.kt` | `cachedNonNull` / `cachedNullable` / `cachedBoolean` for the `@Volatile` caching pattern |
| `JavaLiteralParser.kt` | Shared literal parsing (integer/long/float/double/unescape) |
| `ConstantEvaluator.kt` | Java field initializer constant evaluation (JLS §15.29 subset) |
| `utils.kt` | Misc shared helpers, `computeTypeParameters` factory |

## Reference Implementations

Check these BEFORE implementing new features:

| Feature | javac-wrapper | PSI-based |
|---------|---------------|-----------|
| Type resolution | `TreeBasedClassifierType` | `JavaClassifierTypeImpl` |
| Annotation args | `TreeBasedAnnotation` | `annotationArgumentsImpl.kt` |
| Supertypes | `TreeBasedClass.supertypes` | `JavaClassImpl.supertypes` |
| Type arguments | `TreeBasedClassifierType.typeArguments` | `JavaClassifierTypeImpl.typeArguments` |

**Paths**:
- javac-wrapper: `compiler/javac-wrapper/src/org/jetbrains/kotlin/javac/wrappers/`
- PSI-based: `compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/java/structure/impl/`

## Shared FIR Files (modify with caution)

- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` -- Java class -> FIR conversion
- `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` -- type conversion, raw type detection
- `compiler/fir/fir-jvm/src/.../javaAnnotationsMapping.kt` -- annotation resolution
- `core/compiler.common.jvm/src/.../load/java/structure/*.kt` -- Java model interfaces

---

## Java Language Implicit Rules

- **Interface fields**: implicitly `public static final`
- **Interface methods without body**: implicitly `public abstract`
- **Nested interfaces/enums**: implicitly `static` (even without keyword)
- **Nested classes in interfaces**: implicitly `static` (JLS 9.5)
- **Nested classes**: only static if explicitly marked

## KMP Parser & AST Edge Cases

- **AST representation**: The module uses `JavaLightTree` (flat-array LightTree) with
  `JavaLightNode` value-class handles, not a materialized tree. `getChildren()` results
  are memoized per node via `JavaLightTree.childrenCache`; prefer `findChildByType` /
  `getChildrenByType` (direct-scan on cache miss, filtered fast path on hit).
- **Reserved words in imports**: `import kotlin.*` may parse as `ERROR_ELEMENT`, not `IMPORT_STATEMENT`
- **Fragmented imports**: Parser may split constructs across sibling nodes
- **Recovery needed**: `ERROR_ELEMENT` nodes often contain recoverable info
- **Token naming mismatch**: Parser library defines `SEALED_KEYWORD` constant but produces `SEALED` token in AST. Always verify actual token names via exception-based AST dumping.

## FIR Integration Points

- **Type conversion**: `JavaTypeConversion.kt` - handles `classifier==null` for external types
- **Raw types**: Must create `ConeRawType` for proper method inheritance semantics
- **Flexible types**: Two calls - lower bound with erased args, upper bound with star projections
- **TYPE_USE annotations**: Annotations from method modifier list need filtering before attaching to return type

---

## Common Fixes Reference

| Issue | Solution |
|-------|----------|
| `MISSING_DEPENDENCY_CLASS: 'T'` | Type parameter not in scope - use `resolutionContext.withTypeParameters()` |
| `MISSING_DEPENDENCY_CLASS: 'Outer.Inner'` | Nested class in binary - need to resolve outer first, then lookup nested |
| Raw type errors | Check `isRaw` detection, ensure `ConeRawType.create()` wrapping in FIR |
| Annotation not resolved | Use callback pattern via `resolveAnnotation(tryResolve)` |
| `IR annotation has null argument` (literal) | `JavaAnnotationArgument` must implement value subinterfaces (Literal/Array/Enum/etc) |
| `IR annotation has null argument` (const val) | `REFERENCE_EXPRESSION` for const val needs special handling, not enum |
| `UNRESOLVED_REFERENCE: 'value'` on annotation | Annotation INTERFACE methods need to be exposed (not annotation argument issue) |
| `@Override` on return type | Filter non-TYPE_USE annotations in `JavaTypeOverAst.annotations` |
| Nested interface wrong `isInner` | `isStatic` must return `true` for nested interfaces/enums |
| Nullability check fails | TYPE_USE annotations on type arguments need parsing |

---

*Last updated: 2026-04-21 (refreshed for LightTree + split-file layout)*
