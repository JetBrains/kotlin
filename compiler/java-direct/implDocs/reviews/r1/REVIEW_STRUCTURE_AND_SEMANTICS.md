## Purpose and review basis

This review is based on the current implementation in `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/` together with the design notes in `AGENT_INSTRUCTIONS.md` and `implDocs/`.

The intended goal of the module is clear and coherent: replace IntelliJ-platform/PSI-backed Java parsing for compiler frontend use cases with a KMP-parser-backed implementation that still integrates with FIR and preserves Java↔Kotlin interoperability.

## High-level structure

The module is organized around four layers that mostly line up with the design documents:

1. `parse.kt` and AST utilities
   - Build syntax trees using the KMP Java parser.
   - Provide the raw representation consumed by the java-direct model.

2. Java model over AST
   - `JavaClassOverAst.kt`
   - `JavaMemberOverAst.kt`
   - `JavaTypeOverAst.kt`
   - `JavaAnnotationOverAst.kt`
   - `JavaPackageOverAst.kt`
   - `JavaRecordComponentOverAst.kt`
   - These classes adapt raw AST nodes to the existing `load.java.structure.*` interfaces expected by FIR.

3. Resolution/indexing layer
   - `JavaResolutionContext.kt`
   - `JavaClassFinderOverAstImpl.kt`
   - This is the core of the module: import resolution, local/inner-class lookup, type-parameter scoping, file indexing, package lookup, and cross-file supertype support.

4. Integration layer
   - `CombinedJavaClassFinder.kt`
   - `JavaDirectComponentRegistrar.kt`
   - This layer plugs the implementation into the compiler and combines source-backed lookup with the existing binary finder for JDK and library classes.

This layering is a good fit for the stated objective. The module is not trying to resolve everything directly in the parser or directly in FIR. Instead, it introduces an intermediate Java-model abstraction that mirrors the existing compiler contracts.

## Semantics and design alignment

### What matches the documented goals well

#### 1. FIR remains the final authority for symbol existence

The docs repeatedly emphasize that java-direct should not own full semantic resolution in the Java model. The code follows that principle well:

- `JavaClassifierTypeOverAst.resolve(...)` delegates to `JavaResolutionContext.resolve(...)` and uses a callback.
- `JavaResolutionContext.resolve(...)` returns `ClassId`, not a string.
- The final existence check is deferred to FIR via the callback.

This is one of the module’s strongest design decisions. It keeps java-direct focused on Java name lookup rules while preserving shared FIR behavior.

#### 2. The hybrid source/binary strategy is sound

`CombinedJavaClassFinder` is simple but effective:

- source classes are served by `JavaClassFinderOverAstImpl`
- binaries fall back to the existing platform finder

That is exactly the right boundary for a migration away from PSI dependence. It reduces scope while still making the feature practical.

#### 3. Resolution context as the main semantic carrier is appropriate

`JavaResolutionContext` centralizes:

- package name
- explicit imports
- star imports
- local/top-level class visibility inside a file
- containing-class chain
- type parameters in scope
- inherited type parameters
- optional class-finder access for cross-file navigation

This corresponds closely to the documented “resolution context pattern” and is the right conceptual model for Java source lookup.

#### 4. The implementation reflects real Java language rules rather than parser convenience

Examples:

- implicit enum, annotation, and `java.lang.Object` supertypes in `JavaClassOverAst`
- interface/nested-type visibility and staticness rules in `JavaClassOverAst`
- multi-field declaration handling in `JavaFieldOverAst`
- separate handling for type-use annotations in `JavaTypeOverAst`

These are not superficial details; they show the code is trying to emulate Java semantics rather than merely expose syntax.

## Places where implementation and goals diverge or only partially align

### 1. “Java model is lightweight” is only partially true now

The original design suggests a relatively clean split: AST-backed model, lazy evaluation, and FIR-assisted resolution. In practice, `JavaResolutionContext` has accumulated a large amount of semantic policy and several fallback mechanisms.

The current code does more than “contextual lookup”:

- nested-class disambiguation
- inherited nested-class lookup
- outer-class-chain aggregation
- cross-file ambiguity assistance
- star-import probing
- java.lang / same-package / import prioritization
- partial cross-file visibility heuristics

This is understandable given the problem domain, but it means the module is no longer a very thin adapter layer. It is a genuine mini-resolution engine.

That is not inherently wrong, but it is an important reality gap between the intended simplicity and the current implementation.

### 2. File-scoped model with selective cross-file repair is still architecturally uneven

The docs acknowledge the tension between per-file parsing and cross-file semantic needs, especially around inherited inner classes and ambiguity detection.

The implementation addresses that with:

- caches in `JavaClassFinderOverAstImpl`
- `getDirectSupertypes(...)`
- `collectInheritedInnerClasses(...)`
- extra logic in `JavaResolutionContext`

This works as a pragmatic evolution of the original design, but it also means the architecture is no longer purely file-local. It now relies on a hybrid of file-local contexts plus class-finder-mediated graph traversal.

That is a meaningful semantic expansion from the original concept.

### 3. Some documented design guidance is still encoded as conventions rather than stable abstractions

The docs emphasize callback-based patterns and careful separation of concerns. The code follows that, but often via conventions and comments instead of strong API boundaries. For example:

- the same type may be represented by a local classifier, a `classifierQualifiedName`, or a callback-based `resolve(...)`
- type-use annotation handling depends on callers passing the right annotation groups into the right constructors
- cross-file behavior depends on whether a `classFinderProvider` was threaded through a context

So the design intent is present, but some of it is fragile because it depends on disciplined usage rather than tighter invariants.

## Overall structural assessment

### Strengths

- Good macro-architecture: parser → Java model → resolution context/class finder → FIR integration.
- Strong adherence to the important `ClassId`-based resolution design.
- Practical hybrid source/binary integration.
- Good coverage of tricky Java semantics in several places.
- Clear focus on compatibility with existing FIR interfaces rather than inventing a parallel subsystem.

### Weak points

- `JavaResolutionContext.kt` is oversized and functionally overloaded.
- Cross-file behavior is effective but layered on top of a primarily file-scoped model, which makes the design harder to reason about.
- Several critical semantics depend on distributed conventions instead of a narrower set of stable abstractions.

## Bottom line

The implementation substantially fulfills the initial goal of replacing PSI/platform-backed Java source handling with a direct AST-based model while preserving FIR compatibility. Architecturally, the project direction is sound.

The main caveat is that the semantic core has become significantly more complex than the original “lightweight adapter” framing suggests. The module now succeeds mostly because of careful, pragmatic extensions to the resolution/context/finder layer, not because the problem turned out to be simple.