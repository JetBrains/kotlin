
# Java Facade for Kotlin compiler

The module is intended to replace the PSI-based facade that pulls a lot of IntelliJ platform code into compiler
with a lightweight "direct" implementation.

## Status

The module is functional and integrated into the compiler via the compiler option (`-Xjava-direct`).
The old PSI-based java class finder is still used for binary classes (via `CombinedJavaClassFinder`) due to some
quirks of the FIR providers architecture. On the next iteration it should be replaced with FIR-based symbol providers.

## Purpose

Kotlin has bidirectional Java interop, meaning that in a module there could be both Java files referencing Kotlin declarations
and vice versa. Therefore, Kotlin compiler cannot rely on Java files being available in a binary form, when the Kotlin sources
are compiled and need to process Java sources directly, extract the declarations and make them accessible for the FIR resolution.
This was implemented initially via the infrastructure from the IntelliJ platform, often referred to as PSI-based Java facade.
(There was also an attempt to implement it via unofficial `javac` APIs, but it wasn't properly supported.)

## Architecture

### Output

The module provides a so-called "Java model" as the output, that is the implementation of the interfaces defined
in the `org.jetbrains.kotlin.load.java.structure` package in the `core.jvm` module.

The access is provided via the implementation of the `JavaClassFinder` interface.

### Laziness

Since we only need to consider Java declarations, which are accessed from FIR resolution, and there could be modules with many Java files
and very little interop in this direction, the implementation is made as lazy as possible. It starts with source roots analysis:
for the directory-based roots we consider that the directory structure should correspond to the package structure and only access
files when the corresponding package is requested. For the file-based roots the files are scanned without parsing to extract the
package name and top-level classes and then parsed only if later requested from FIR.
Parsing is done eagerly into a light-tree structure (see below), but further extraction of the Java Model is also done lazily with some
caching on top.

### Parser

The module uses lightweight KMP parsers infrastructure being developed in the IntelliJ platform, but without pulling the heavy
parts of the platform with it. The libraries (`org.jetbrains:syntax-api` and `org.jetbrains:java-syntax`) are extracted and published
independently by the Fleet team.

The parsing produces a light-tree structure similar to the Kotlin light-tree parser, but without any use of the IntelliJ platform-specific  
infrastructure.

### Java model entry point

`JavaClassFinderOverAstImpl` - main entry point: finds sources-based classes and packages by FqName. It implements the `JavaClassFinder`
interface, which is used by Fir for accessing all Java declarations, including source-based ones.

It retuns Java model entities: classes in the `org.jetbrains.kotlin.java.direct.model` package.
In most cases they use parsed data to lazily construct sub-elements, but in cases where some external (to this element) entities are
required, the resolution mechanism (see below) is used to find these elements and expose it accordingly.

### Resolution

The Java model requires symbol resolution, for many cases, such as references to Java source declarations in the same module, references
to library declarations, and references to Kotlin declarations. These "external" declarations are expected by the model to be "resolved",
i.e., expressed in the terms of the Java model and exposed accordingly. E.g. `JavaClass.supertypes` is expected to return a collection of
`JavaClassifierType`s regardless of whether those are Java declarations in the same set of java sources, binary java classes or Kotlin
declarations for Kotlin sources in the same module.

In contrast to the PSI-based facade, the `java-direct` module uses FIR-based resolution for all non-Java-sources references, via the
FirSession stored in the class finder. Then the resulting Fir is wrapped to the Java model representation (see `FirBackedJava*` classes).

#### Main entities participating in the resolution

- classes in the `org.jetbrains.kotlin.java.direct.model` are main entry points to the resolution: they call other helpers at the point they need to resolve the "external" declarations.
- `JavaResolutionContext` is the current declaration context information required for resolving names referenced by the declaration; it contains information collected from an enclosing file as well as a semantical "scope" (see below)
- `JavaTypeResolver.kt` is a set of helpers for resolving names (strings) to `ClassId`s, `FqName`s and values
- `JavaScopeResolver.kt` is an additional set of helper for resolving names to Java model entities in the current scope (type parameters (including inherited), containing class, top level classes from the same file, nested classes)
- `JavaImportResolver.kt` is a holder + extractors for resolved imports
- `JavaInheritedMemberResolver` - supertype-hierarchy traversal for inherited member types
- `JavaExternalConstResolver.kt` - set of helper for accessing const values via `FirExpressionEvaluator`
- `JavaModelSessionAccess.kt` - cycle breakers for the resolution logic + `TYPE_USE` annotations cache
- `FirBackedJavaClassAdapter` - an adapter from ClassId to `JavaClass`, lazily resolved via Fir

#### Main resolution scenarios

### Scenario A — Classifier for a type reference (model entry dispatcher)

Entry: `JavaTypeOverAst.computeClassifier`

1. Split the reference into `rawTypeNameParts` (identifiers only; annotations / `<...>` dropped).
2. If single-part, try in priority order and return the first hit:
    1. own type parameter — `JavaScopeResolver.findTypeParameter` (high priority).
    2. in-scope class — `JavaScopeResolver.findClassInCurrentScope` (Scenario C).
    3. inherited (outer) type parameter — `findInheritedTypeParameter` (low priority, shadowed by 2).
3. Resolve `parts[0]` via `findClassInCurrentScope`. If it is an AST `JavaClass`, navigate each
   remaining part with `declaredOrSameFileInherited` and return the final inner class (same-file
   AST path).
4. Otherwise (cross-file) resolve the whole name to a `ClassId` via `JavaTypeResolver.resolve`
   (Scenarios B/D) and wrap it in a `FirBackedJavaClassAdapter` (`classifierAdapterFor`).
5. If nothing matched, return `null` (FIR's `findClassId` fallback then runs).

Corner cases: type-parameter-vs-inner-class shadowing (2 before 3); same-file multi-segment
navigation handled purely on AST without touching the symbol provider.

### Scenario B — Simple name to `ClassId` (JLS 6.4.1 shadowing ladder)

Entry: `JavaTypeResolver.resolve` → `resolveSimpleNameToClassIdImpl`. A flat ordered ladder; each
step probes candidate `ClassId`s through `tryResolve` and returns the first hit.

1. **Local scope** (`resolveFromLocalScope`) — member types declared *and* inherited by the
   containing-class chain, walked innermost→outermost, interleaving declared and inherited per
   level (Scenario D for the inherited part). *(skipped in the reentrance-safe flavor)*
2. **Same-file top-level** (`resolveFromSameFile`) — via `sameFileTopLevelClassProvider`.
3. **Single-type import** (`resolveFromExplicitImport`) — `import a.b.C;`, rank 4.
4. **Single-static import, type arm** (`resolveFromStaticSingleImport`) — `import static a.b.C.X;`,
   rank 4, probed after step 3.
5. **Same-package, other file** (`resolveFromSamePackage`) — `ClassId(package, name)`.
6. **`java.lang.*`** (`resolveFromJavaLang`) — implicit import; also accepts a
   `JavaToKotlinClassMap` hit.
7. **Type-import-on-demand** (`resolveFromTypeStarImports`) — `import a.b.*;`, rank 6; falls back to
   member types of an imported *class* (`import a.D.*`).
8. **Static-import-on-demand** (`resolveFromStaticStarImports`) — `import static a.b.C.*;`, rank 7.

Corner cases: rank-4 type import probed before rank-4 static import; star-import ambiguity →
`null`; the class-as-`PackageOrTypeName` fallback in steps 7–8.

### Scenario C — In-scope (AST) classifier lookup

Entry: `JavaScopeResolver.findClassInCurrentScope`. AST-only; produces a structural `JavaClass`
with its full outer chain (needed for navigation and outer-arg substitution).

1. Inner class **declared or same-file-inherited** by the containing class
   (`declaredOrSameFileInherited` → `findInnerClassInSameFileSupertypes`).
2. Inner class **inherited from supertypes** of the containing class
   (`JavaInheritedMemberResolver.findInnerClassFromSupertypes`) — runs before step 3 because an
   inherited member type shadows a merely lexically-enclosing one (JLS 6.4.1).
3. Sibling inner class of the immediate outer class.
4. Inner class of each further outer class up the containing chain.
5. Same-file top-level class (`sameFileTopLevelClassProvider`).

Corner case: the same-file supertype walk works on **raw AST text**
(`directSupertypeRefNames`), deliberately distinct from the resolved-classifier walk in
`JavaInheritedMemberResolver`, to avoid re-entering type construction; package-qualified
supertypes are declined here and handed to the `ClassId` path.

### Scenario D — Qualified / nested name to `ClassId` (JLS 6.5.2)

Entry: `JavaTypeResolver.resolve` (dotted name) → `resolveQualifiedNameToClassIdFromParts`.
A single left-to-right pass mirroring javac's PackageOrTypeName classification (JLS 6.5.4):

1. **Leftmost type** (JLS 6.5.4): the first segment as a simple type name in scope (Scenario B);
   failing that, the package prefix grows one segment at a time until a segment names a
   top-level type in it (`java.util.List` → packages `java`, `java.util`, type `List`).
2. **Member-type descent** (JLS 6.5.5.2): each remaining segment must be a member type of the
   previous one — declared, or inherited from its supertypes (`findInheritedNestedClass`,
   supertype walk + finder).

### Scenario E — Inherited member type via supertypes

Entry: `JavaInheritedMemberResolver`. Two complementary outputs:

- `findInnerClassFromSupertypes` → a `JavaClass` with AST outer chain (for the AST pipeline /
  outer-arg substitution); uses same-file supertypes plus the `LeanJavaClassFinder` for cross-file
  Java source.
- `resolveInheritedInnerClassToClassId` → a bare `ClassId` via a two-pass BFS:
    1. **`walkJavaSourceSupertypes`** — Java-source supertypes through the finder's source index,
       resolving each level against *that file's* imports; independent of FIR lazy phases.
    2. **`walkBinarySupertypes`** — Kotlin / binary supertypes through the per-origin
       `directSupertypeClassIds` dispatcher (Scenario F).

Both passes share a `visited` set, detect cross-pass ambiguity (→ `null`), and are bounded by
`MAX_SUPERTYPE_DEPTH = 5`.

### Scenario F — Direct-supertype `ClassId` graph

Entry: `JavaTypeResolver.directSupertypeClassIds`, guarded by `cycleGuardedSupertypeWalk`.
Per-origin dispatch:

1. **Source Java** — finder has the class in its index: walk `JavaClass.supertypes` and read each
   `classifier.classId` (no FIR phase).
2. **Binary Java** — symbol is a `FirJavaClass`: read the pre-resolved
   `directSupertypeClassIds()` cache (never triggers enhancement).
3. **Kotlin / built-in / deserialized** — `lazyResolveToPhase(SUPER_TYPES)` then read
   `superTypeRefs` cone class ids.

Corner case: `Java.Source` (lazy `superTypeRefs`) must be distinguished from `Java.Library`
(pre-populated) to avoid premature-resolution cycles — handled by routing source Java through
the finder arm, not the FIR arm.

### Scenario G — Implicit outer-class type-argument recovery

Entry: `JavaTypeResolver.recoverInheritedOuterTypeArguments`, used by
`JavaTypeOverAst.computeTypeArguments` for a bare inherited inner-class reference whose outer args
are neither written nor lexically in scope.

1. From the lexical containing class, walk **outward** (its outer classes already have supertypes
   resolved).
2. Stop the walk at the first `static` class along the chain (a static nested class severs the
   enclosing-instance chain — JLS).
3. For each outer, descend its `FirBackedJavaClassAdapter.supertypes` looking for the inner
   class's outer `ClassId`, substituting type args down each intermediate class
   (`findTypeArgsForClassInHierarchy` / `substituteTypeArgs`).
4. Return the recovered args as FIR-backed `JavaType`s, or `null` (top-level inner, no containing
   class, static break, or not found).

### Scenario H — Annotation reference resolution

Entry: `JavaAnnotationOverAst`. Reuses the type pipeline: `JavaTypeResolver.resolve` on the
annotation's written name (same import/scope rules as Scenario B/D), yielding the annotation's
`ClassId`. The no-symbol-provider fixtures fall back to a package+name heuristic. TYPE_USE-ness for
filtering is answered by `JavaModelSessionAccess.isTypeUseAnnotationClass` (cached per session,
inspects the annotation class's own `@Target`).

### Scenario I — Cross-language constant value resolution

Entry: `JavaExternalConstResolver`, used by `JavaFieldOverAst.initializerValue` and the
enum-entry-vs-`const val` disambiguation in annotation arguments.

- `resolveExternalFieldValue(qualifier, field)` — tries, in order: top-level property via JVM
  facade (`MainKt.FOO`), class member, companion-object member; returns the evaluated literal.
- `resolveConstFieldValue(classId, field)` — enum class → companion only; otherwise class member
  then companion then top-level facade fallback.

Const values are read via `FirExpressionEvaluator` / already-evaluated initializers; unqualified
cross-language references are unsupported (return `null`).

### Integration

The new java facade builder is introduced to allow substituting the implementation depending on the compiler option.

### Tests

The module contains unit tests and also "steals" all phased diagnostics and box tests that contain Java files from the main compiler
testdata.
