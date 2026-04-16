# Review 1: Overall Structure, Semantics, and Correspondence to Initial Goals

## 1. Initial Goals Recap

Per the design meeting (2025-11-20) and IMPLEMENTATION_PLAN.md, the java-direct module aims to:

1. **Replace IntelliJ PSI-based Java parsing** with a custom implementation using the KMP Java Parser (`com.intellij.java.syntax`), eliminating the IJ platform dependency.
2. **Implement a new Java Model** (not generate FIR directly) — providing `JavaClass`, `JavaMethod`, `JavaField`, etc. backed by AST nodes instead of PSI.
3. **Delegate resolution to FIR** — the Java Model provides names and structure; FIR resolves them via `session.symbolProvider`.
4. **Use a callback pattern** for star imports and cross-file resolution, so java-direct handles Java scoping rules while FIR validates class existence.
5. **Support hybrid class finding** — source classes via java-direct, binary/JDK classes via the existing platform finder.
6. **Maintain laziness** — parse and resolve only when needed.

---

## 2. Module Structure

The module consists of 14 source files (~4,500 lines total), organized into clear functional layers:

### Parsing Layer
- **`parse.kt`** (40 lines) — Thin wrapper around the KMP Java Parser. Creates a `SyntaxTreeBuilder` from source text using `JavaSyntaxDefinition` at `LanguageLevel.HIGHEST`.
- **`utils.kt`** (128 lines) — Defines `JavaSyntaxNode` (the AST node wrapper), the `buildSyntaxTree` function that converts `SyntaxTreeBuilder` output into a navigable tree, and helper extensions (`findChildByType`, `getChildrenByType`). Also contains `computeTypeParameters` for the two-phase type parameter construction pattern.

### Java Model Layer (implements `core/compiler.common.jvm/.../load/java/structure/*.kt` interfaces)
- **`JavaElementOverAst.kt`** (20 lines) — Base class; wraps a `JavaSyntaxNode`, provides identity via node equality.
- **`JavaClassOverAst.kt`** (295 lines) — `JavaClass` implementation. Handles modifiers, supertypes (including implicit ones for enums/annotations), inner class lookup (including supertype inheritance per JLS 8.5), sealed class permitted types, and member extraction.
- **`JavaMemberOverAst.kt`** (395 lines) — `JavaMethod`, `JavaField`, `JavaConstructor`, `JavaValueParameter` implementations. Handles multi-field declarations, enum constants, interface implicit modifiers, annotation method defaults, and constant initializer detection.
- **`JavaTypeOverAst.kt`** (649 lines) — `JavaClassifierType`, `JavaPrimitiveType`, `JavaArrayType`, `JavaWildcardType`, `JavaTypeParameter` implementations. Handles type name extraction from AST, `classifier` resolution (type params → inner classes → inherited params), `classifierQualifiedName` computation, raw type detection, type argument collection (including implicit outer type params for inner classes), and TYPE_USE annotation filtering.
- **`JavaAnnotationOverAst.kt`** (459 lines) — `JavaAnnotation` and all `JavaAnnotationArgument` subinterfaces (literal, array, enum, class object, nested annotation). Includes literal parsing, constant expression evaluation for annotation values, and Java string unescaping.
- **`JavaRecordComponentOverAst.kt`** (32 lines) — `JavaRecordComponent` implementation for Java records.
- **`JavaPackageOverAst.kt`** (34 lines) — `JavaPackage` implementation delegating to the class finder.

### Resolution Layer
- **`JavaResolutionContext.kt`** (1,011 lines) — The core resolution engine. Encapsulates package name, imports, type parameter scope, containing class chain, and class finder reference. Implements JLS-compliant resolution priority (explicit imports → local/inner classes → inherited inner classes → same package → java.lang → star imports). Handles nested class resolution, cross-file ambiguity detection, and the `resolve()` callback pattern.

### Infrastructure Layer
- **`JavaClassFinderOverAstImpl.kt`** (641 lines) — `JavaClassFinder` implementation. Indexes source roots, manages class cache, implements two-tier indexing (small files parsed eagerly, large files scanned lightweight), and provides supertype chain traversal for cross-file ambiguity detection.
- **`CombinedJavaClassFinder.kt`** (84 lines) — Hybrid finder: source-first, binary-fallback. Includes FQN verification for binary results.
- **`JavaDirectComponentRegistrar.kt`** (65 lines) — Compiler plugin registration. Wires up the `JavaClassFinderFactory` extension point.
- **`ConstantEvaluator.kt`** (380 lines) — Evaluates Java constant expressions in field initializers. Supports literals, binary/unary operations, field references (local and cross-language via callback).

---

## 3. Correspondence to Goals

### ✅ Goal 1: Replace PSI-based parsing
Fully achieved. The module uses `com.intellij.java.syntax` (KMP Java Parser) exclusively. No PSI dependency exists in the module. The `JavaSyntaxNode` wrapper provides a clean abstraction over the parser's output.

### ✅ Goal 2: Implement a new Java Model
Fully achieved. All key `JavaClass`, `JavaMethod`, `JavaField`, `JavaType`, `JavaAnnotation` interfaces are implemented over AST nodes. The implementations closely mirror the PSI-based reference implementations (`JavaClassImpl`, `JavaClassifierTypeImpl`, etc.) in behavior while using AST navigation instead of PSI resolution.

### ✅ Goal 3: Delegate resolution to FIR
Achieved via the callback pattern. `JavaClassifierType.resolve(tryResolve)` and `JavaAnnotation.resolveAnnotation(tryResolve)` let FIR validate class existence through `symbolProvider`. The Java Model never accesses `FirSession` directly. `classifierQualifiedName` is informational only; `resolve()` returns `ClassId` for precise resolution.

### ✅ Goal 4: Callback pattern for resolution
Well-implemented. Five distinct callback patterns are established (type resolution, annotation resolution, enum class resolution, TYPE_USE filtering, constant evaluation). The `JavaResolutionContext.resolve()` method implements JLS-compliant priority ordering and returns `ClassId` to avoid string ambiguity.

### ✅ Goal 5: Hybrid class finding
Achieved via `CombinedJavaClassFinder`. Source classes are found through `JavaClassFinderOverAstImpl`'s index; binary classes fall back to the platform finder. The `isClassInIndex` fast path avoids unnecessary source finder calls.

### ✅ Goal 6: Laziness
Largely achieved. Type parameters, supertypes, members, annotations, and inner classes are all computed lazily via `by lazy(LazyThreadSafetyMode.PUBLICATION)`. The two-tier indexing strategy defers full parsing of large files. Class instances are cached to avoid re-parsing. However, small files (≤4KB) are parsed eagerly during index build, which is a deliberate trade-off for cache-hit performance.

### ⚠️ Partial: Production readiness (Milestone 5)
Per IMPLEMENTATION_PLAN.md, the module is at ~95.9% test pass rate (Milestone 4 complete). Modern Java features (records, sealed classes) have basic support. Remaining work includes edge cases (~60 tests) and performance optimization.

---

## 4. Semantic Correctness Assessment

### JLS Compliance
The resolution pipeline follows JLS rules faithfully:
- **JLS 6.5.2**: Nested class interpretation takes priority over package interpretation when the outer name resolves to a class.
- **JLS 7.5.1**: Explicit single-type imports have highest priority.
- **JLS 8.1.1.1**: Interface/annotation abstractness rules are correctly implemented.
- **JLS 8.5**: Inherited member types are resolved through supertype hierarchy traversal.
- **JLS 8.9**: Enum implicit modifiers (public static final for constants, implicit Enum supertype) are handled.
- **JLS 9.5**: Nested types in interfaces are implicitly static and public.

### Cross-File Resolution
The module handles cross-file scenarios through:
1. `JavaClassFinderOverAstImpl.collectInheritedInnerClasses()` for supertype hierarchy traversal
2. `JavaResolutionContext.resolveInheritedInnerClassToClassId()` with BFS through both Java model and FIR callbacks
3. Ambiguity detection (multiple candidates → return null → MISSING_DEPENDENCY_CLASS error)

### Key Design Insight
The `classifier` property returns `null` for external classes (cross-file, JDK, libraries). This is by design — FIR handles these through the `resolve()` callback. The `isTriviallyFlexibleHint` property compensates for the missing `classifier` to ensure correct flexible type rendering.

---

## 5. Summary

The java-direct module successfully implements its stated goals. The architecture is clean, with well-separated layers (parsing → model → resolution → infrastructure). The callback pattern elegantly bridges the Java Model and FIR layers without introducing coupling. The resolution pipeline is JLS-compliant and handles complex scenarios (nested classes, cross-file inheritance, ambiguity detection). The main gap is the remaining ~4% of failing tests, which appear to be edge cases rather than fundamental architectural issues.
