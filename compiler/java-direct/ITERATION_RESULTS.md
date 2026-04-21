# Java-Direct: Iteration Results Log

**Current status**: 1168/1168 box + 1454/1456 phased (2679/2681, 99.9%), 2 known won't-fix.

**Last Updated**: 2026-04-21 (refactoring-plan Phase B.3: split large functions)

### Open performance items (remaining after the 2026-04-20 / 04-21 perf work)

The detailed `PERFORMANCE_REVIEW_2026-04-20.md` is now in `implDocs/archive/` — every Quick Win
(§2 #1–#5 and #7), §4 "overkill" `ConcurrentHashMap` downgrades, and §6 thread-safety
caveats have landed in code (verified against current source). What remains:

- **§2 #6** — context-level `tryResolve` cache across `resolve()` calls. Deferred with
  a recorded correctness argument (see this file's 2026-04-20 entry).
- ~~**§2 #8** — profile `SMALL_FILE_SIZE_THRESHOLD` (still 4096, unvalidated). Low value.~~
  Rolled into `REFACTORING_PLAN.md` Phase C (item M-O6).
- ~~**§5 architectural** — lazy per-package indexing and AST release after extraction.~~
  **Done** (lazy indexing landed 2026-04-21; AST release after extraction is a separate item).

The broader follow-up plan is tracked in `REFACTORING_PLAN.md` (Phases A–E). Phase A and
the three iterations of Phase B (B.1, B.2, B.3) are now complete; Phases C–E are pending.

---

## Refactoring Plan Phase B.3: Split Large Functions — 2026-04-21

### Overview

Third and final iteration of Phase B. Addresses `REFACTORING_PLAN.md` items **R5** (split
`buildJavaLightTree`), **R10** (rename cryptic identifiers), **R7** (split
`resolveInheritedInnerClassToClassId`), **R8** (extract "find TYPE node and optional star"
helper), and **R12 + O10** (split `resolveSimpleNameToClassIdImpl` and extract the
`checkInheritance = false` flavor). Pure readability work — each edit is an
in-file factoring with unchanged behaviour.

### Changes

- **R5** (`JavaLightTree.kt`) — `buildJavaLightTree` shrank from ~220 inline lines to a
  ~40-line orchestrator that allocates the output arrays and calls three new file-private
  top-level functions:
  - `buildCompositeIndices(...)` — Pass 1: stack-walks production markers to produce
    `parentStartIndex` / `doneForStart` / `compositeTypes` / `errorFlags` /
    `compositeStartOffsets` / `compositeEndOffsets`.
  - `assignTokenParents(...)` — Pass 2: interleaves the marker walk with the token stream
    to populate `tokenParentStart`.
  - `buildChildrenIndex(...)` — Pass 3: precomputes every composite's children list plus
    the synthetic root's via a **generalized** `buildChildrenFor` that accepts
    `startIdx = -1` for the root case. A single `val slot = if (startIdx < 0) rootIndex
    else startIdx` guard picks the output slot, and the existing `i = startIdx + 1`
    starting condition already yields `i = 0` for the root — so the 27-line duplicate
    synthetic-root block disappears entirely.
- **R10** — renamed cryptic identifiers:
  - `JavaClassFinderOverAstImpl.kt:123, 136` — the `fri` local in the init block became
    `fileRootIndexBuilder` (mirroring the field name `fileRootIndex` it feeds into).
  - `JavaClassFinderOverAstImpl.kt` (seven sites) — every `byName` local (the per-package
    map of class simple name → `List<FileEntry>`) became `classesByName`.
  - `JavaTypeOverAst.kt:109, 115` — the single-letter `n: JavaLightNode` parameters on
    `extractTypeName` / `collectIdentifiers` became `node`.
  - `JavaLightTree.kt` (inside the new `assignTokenParents`) — the `top2` / `push2` /
    `pop2` suffix is gone; once Pass 2 lives in its own function, the `2` was pure
    accidental naming and drops to `top` / `push` / `pop`.
- **R7** (`JavaInheritedMemberResolver.kt`) — `resolveInheritedInnerClassToClassId`
  became a short driver that collects `initialSupertypes` and delegates to two new
  private methods:
  - `findInPhase1JavaModel(...)` — BFS through `JavaClassifierType` objects from the
    Java model; populates `nonSourceSupertypeIds` for Phase 2.
  - `findInPhase2ClassIdWalk(...)` — deque-based BFS over the ClassIds of non-source
    supertypes using the `getSupertypeClassIds` callback.
  Both phases share `visited` (so cross-phase ambiguity detection still works) and the
  new `MAX_SUPERTYPE_DEPTH = 5` companion constant (replacing an inline `maxDepth` local).
- **R8** (`JavaImportResolver.kt`) — extracted the nested "find TYPE node and optional
  star" lookahead (~25 lines) out of `extractFragmentedImports` into a new
  `findTypeNodeAndStar(tree, allChildren, startIdx): FragmentedImportTarget?` helper
  plus a `FragmentedImportTarget(typeNode, hasStar)` data class. The main loop no longer
  juggles indices and lookahead at the same time.
- **R12 + O10** (`JavaResolutionContext.kt`) — `resolveSimpleNameToClassIdImpl` now reads
  as a linear sequence of five `try*` helpers matching the JLS 6.5.2 scoping steps:
  `tryImport` (step 1), `tryLocalAndInherited` (steps 2 + 2b), `trySamePackage` (step 3),
  `tryJavaLang` (step 4), `tryStarImports` (step 5). Two of those take a
  `checkInheritance: Boolean` — `tryImport` (nested vs. simple FQN split) and
  `tryStarImports` (ambiguity + class-level handling vs. linear probe) — because the
  two modes are fundamentally different there; steps 2/2b only run in the true mode.
  A new thin `resolveSimpleNameToClassIdWithoutInheritance` workhorse composes the four
  inheritance-independent steps, and a parallel
  `resolveNestedClassToClassIdFromPartsWithoutInheritance` replaces the former
  `checkInheritance = false` branches of `resolveNestedClassToClassIdFromParts`. The
  `checkInheritance` parameter on `resolveNestedClassToClassIdFromParts` and all its
  conditionals are gone — the method is now single-mode, called only from
  `resolveNestedClassToClassId`. The `resolveWithoutInheritance` callback inside
  `resolveInheritedInnerClassToClassId` routes through the two `WithoutInheritance`
  methods directly; no boolean flags leak into the callback site.

### Files Modified

| File | Item(s) |
|------|---------|
| `JavaLightTree.kt` | R5 (buildJavaLightTree split + generalized `buildChildrenFor`); R10 (pass-2 helpers drop `2` suffix) |
| `JavaClassFinderOverAstImpl.kt` | R10 (`fri` → `fileRootIndexBuilder`, `byName` → `classesByName` across 7 sites) |
| `JavaTypeOverAst.kt` | R10 (`n` → `node` on `extractTypeName` / `collectIdentifiers`) |
| `JavaInheritedMemberResolver.kt` | R7 (extract `findInPhase1JavaModel` + `findInPhase2ClassIdWalk`; `MAX_SUPERTYPE_DEPTH` companion constant) |
| `JavaImportResolver.kt` | R8 (extract `findTypeNodeAndStar` + `FragmentedImportTarget` data class) |
| `JavaResolutionContext.kt` | R12 + O10 (extract `tryImport` / `tryLocalAndInherited` / `trySamePackage` / `tryJavaLang` / `tryStarImports`; add `resolveSimpleNameToClassIdWithoutInheritance` + `resolveNestedClassToClassIdFromPartsWithoutInheritance`; drop `checkInheritance` parameter from `resolveNestedClassToClassIdFromParts`) |

### Test Results

Gate per `REFACTORING_PLAN.md` §2.1:
```
./gradlew :kotlin-java-direct:test \
  --tests JavaUsingAstPhasedTestGenerated \
  --tests JavaUsingAstBoxTestGenerated \
  --tests JavaParsingTest \
  --rerun-tasks
```
→ **BUILD SUCCESSFUL**, `tests=2671 failures=0 errors=0 skipped=0`. Identical count and
outcome to the Phase B.2 baseline. IDE `get_file_problems` on every edited file reports
0 errors (pre-existing style warnings only).

### Key Learnings

- **A "can't quite share this" duplication usually can, with one parameter or one
  conditional.** The synthetic-root children block in `buildJavaLightTree` looked like it
  genuinely needed its own copy because the main helper's `var i = startIdx + 1` didn't
  support a "start from 0" semantics. Actually, passing `startIdx = -1` yields the same
  `i = 0` and the only real difference was the output slot — one `if (startIdx < 0)
  rootIndex else startIdx` line. Before committing to "this path can't reuse the helper",
  it pays to try substituting the sentinel value and see what actually breaks.
- **Boolean parameters that gate multiple independent branches are a splitting signal.**
  `resolveSimpleNameToClassIdImpl`'s `checkInheritance` parameter gated steps 1, 2, 2b,
  and 5 — four distinct branches. After splitting into named `try*` helpers, only two of
  them still take the flag (steps 1 and 5, where the two modes really are different);
  steps 2/2b moved under an `if (checkInheritance) { tryLocalAndInherited(...) }` guard
  in the caller, which reads better than a parameter buried inside the helper. When the
  flag gates everything uniformly, keep it; when it gates discrete branches, split the
  branches.
- **Accidental suffixes outlive the scope that motivated them.** `push2` / `pop2` / `top2`
  existed because Pass 1's `push` / `pop` / `peekOrRoot` lived in the same function
  scope. The `2` was solved-at-one-moment disambiguation that never got cleaned up.
  Extracting Pass 2 into its own function erases the name clash and the suffix
  simultaneously — a reminder that renames and scope extractions are often the same
  refactor.
- **Two-phase algorithms want two names.** `resolveInheritedInnerClassToClassId` did
  Phase 1 (Java model BFS) and Phase 2 (ClassId BFS via FIR callback) in one body
  separated only by a comment. Extracting them into `findInPhase1JavaModel` and
  `findInPhase2ClassIdWalk` makes the hand-off explicit: Phase 1 writes
  `nonSourceSupertypeIds`, Phase 2 reads it; the outer driver picks up the
  "Phase-1-returned-null means continue, not give up" contract in one visible place
  instead of hidden in a BFS loop's fall-through.

---

## Refactoring Plan Phase B.2: Consolidate Annotation-Arg Evaluation — 2026-04-21

### Overview

Second iteration of Phase B. Addresses `REFACTORING_PLAN.md` items **O7** (duplicated
mini-evaluator in `JavaAnnotationOverAst`) and **R11** (two TODOs flagging unfinished
thought in the same file). The two evaluators — `ConstantEvaluator` (field initializers)
and the annotation-argument path — previously each carried their own copy of literal
evaluation plus a divergent numeric-operator implementation. The annotation variant
silently truncated all numeric ops through `Long`, dropping Float/Double precision.

Both evaluators now share the literal and numeric-operator primitives via `JavaLiteralParser`.

### Changes

- **`JavaLiteralParser.kt`** gained two new `object`-level functions:
  - `evaluateLiteral(node: JavaLightNode, tree: JavaLightTree): Any?` — turns a
    `LITERAL_EXPRESSION` node into its Java value (String/Char/Boolean/Number/null).
    Mirrors the previous `ConstantEvaluator.evaluateLiteral` exactly; the fallback
    `text.toIntOrNull() ?: text.toLongOrNull() ?: text.toDoubleOrNull() ?: text` branch
    from the annotation variant is dropped — per JLS 3.10 annotation literal tokens are
    well-typed, and the fallback was masking parser bugs rather than helping (answers the
    `// TODO: check against specs` pointer).
  - `evaluateNumericBinaryOp(lhs: Number, operator: SyntaxElementType, rhs: Number): Any?`
    — Java-ish numeric promotion (`Double → Float → Long → Int`) across arithmetic,
    bitwise, shift, equality, and comparison operators. Extracted verbatim from the old
    `ConstantEvaluator.evaluateNumericOp`; returns `null` for operators it doesn't know.
- **`ConstantEvaluator.kt`** — `evaluateLiteral` is now a one-liner delegating to
  `JavaLiteralParser.evaluateLiteral(node, tree)`. `evaluateNumericOp` deleted; the four
  call sites inside `evaluateBinaryOp` (Number/Number, Char/Number, Number/Char, Char/Char)
  now call `JavaLiteralParser.evaluateNumericBinaryOp`. Net: −70 lines.
- **`JavaAnnotationOverAst.kt`** — deleted the private `evaluateLiteral(node, tree)`,
  the `numericBinaryOp(left, right, op)` helper, and the `ANNOTATION_BINARY_OPERATOR_TYPES`
  operator set. `evaluateConstantExpression`'s `LITERAL_EXPRESSION` branch now calls
  `JavaLiteralParser.evaluateLiteral`; the `BINARY_EXPRESSION` branch reads
  `children[0..2]` directly (Phase A's whitespace-free children make the old
  filter-out-operators dance unnecessary) and delegates numeric cases to
  `JavaLiteralParser.evaluateNumericBinaryOp` via a tiny `evaluateAnnotationBinaryOp`
  helper that adds the annotation-only `String + x` concatenation special case on top.
  Removes both TODOs. Net: −65 lines.

Behaviour change worth noting: annotation-argument numeric operations that previously
truncated Float/Double to Long now return the correctly typed result. No test broke —
existing annotation tests use integer-typed values — but the change is strictly more
correct per JLS 15.19 (numeric promotion in compile-time constant expressions).

### Files Modified

| File | Change |
|------|--------|
| `JavaLiteralParser.kt` | Added `evaluateLiteral` + `evaluateNumericBinaryOp`; new `com.intellij.java.syntax.element.JavaSyntaxTokenType` / `com.intellij.platform.syntax.SyntaxElementType` imports |
| `ConstantEvaluator.kt` | `evaluateLiteral` delegates; `evaluateNumericOp` deleted; four `evaluateBinaryOp` call sites switched to shared helper |
| `JavaAnnotationOverAst.kt` | Deleted private `evaluateLiteral`, `numericBinaryOp`, `ANNOTATION_BINARY_OPERATOR_TYPES`; `evaluateConstantExpression` + new `evaluateAnnotationBinaryOp` now route through `JavaLiteralParser`; two TODOs removed |

### Test Results

Gate per `REFACTORING_PLAN.md` §2.1:
```
./gradlew :kotlin-java-direct:test \
  --tests JavaUsingAstPhasedTestGenerated \
  --tests JavaUsingAstBoxTestGenerated \
  --tests JavaParsingTest \
  --rerun-tasks
```
→ **BUILD SUCCESSFUL**, `tests=2671 failures=0 errors=0 skipped=0`. Identical count and
outcome to the Phase A / B.1 baseline. IDE `get_file_problems` on every edited file
reports 0 errors (pre-existing style warnings only).

### Key Learnings

- **Two copies of an evaluator drift silently.** The annotation's `numericBinaryOp`
  truncated everything through `Long`, losing Float/Double precision — a bug that tests
  never caught because real-world annotation values are almost always integer. A shared
  helper forces the question "do these really need to differ?" at the API boundary and
  prevents the kind of silent divergence that produces incorrect compile-time constants
  only on the annotation path.
- **Whitespace-free children unlock simpler indexing.** Phase A's "exclude WHITE_SPACE
  from children" change made `children[0..2]` a safe direct access for BINARY_EXPRESSION.
  The old annotation code had a `filter` + `firstOrNull` dance specifically to skip
  non-operator tokens; after Phase A, that dance is pure noise. Future refactorings
  should treat whitespace-free children as a given when re-reading older code.
- **Resolving TODOs means answering the question, not deferring it.** The
  `// TODO: check if it needs to be replaced with ConstantEvaluator` question had a
  concrete answer: ConstantEvaluator depends on a containing class, which annotation
  arguments may lack (e.g. annotations on type parameters or packages), so the full
  evaluator cannot be reused as-is. What *is* reusable is the literal + numeric-op
  primitives — and that's exactly what now lives in `JavaLiteralParser`.
- **`JavaLiteralParser` has grown from "literal parsers" into "shared evaluation
  primitives".** The updated KDoc reflects that: the file still hosts the textual parsers
  (`parseIntegerLiteral` etc.) but now also owns the node-level literal evaluation and
  the numeric-op semantics. If a third consumer appears (e.g. enum-value initializer
  evaluation), these primitives should be the starting point.

---

## Refactoring Plan Phase B.1: Deduplicate Walks and Wrappers — 2026-04-21

### Overview

First iteration of Phase B. Five items grouped around the theme "two or more code
fragments doing the same work" (see `REFACTORING_PLAN.md` §2.2 Iteration B.1). The aim is
readability + a small memory/allocation win; no measurements were required because no
tree-walk count changes (confirmed by the test suite running identically after the
changes).

### Changes

- **P1** (`JavaTypeOverAst.kt:30-73`) — introduced a `@Volatile`-cached
  `typePositionAnnotations` that holds `extraAnnotations + modifierListAnnotations +
  directAnnotations`. `override val annotations` now returns
  `memberAnnotations + typePositionAnnotations`; `filterTypeUseAnnotations` returns
  `typePositionAnnotations + memberAnnotations.filter { ... }`. Previously both entry points
  walked `MODIFIER_LIST` and direct-children independently, rebuilding
  `JavaAnnotationOverAst` wrappers on each call. Now the wrapper list is allocated once per
  type instance.
- **P7 + R6** (`JavaTypeOverAst.kt:455-564`) — split `createJavaType` into a top-level
  dispatcher plus three `private` helpers:
  - `tryCreateArrayOrVarargFromTypeNode(typeNode, ..., extraAnnotations, memberAnnotations)`
    handles the array/vararg case and returns `null` when `typeNode` encodes neither. The
    vararg annotation split (component vs. array wrapper) lives in one place now, used by
    both the TYPE-input path and the derived-`typeNode` path — Path 2's formerly-empty
    component annotations become a no-op split (`hasVarargEllipsis = false`) with no
    behavior change.
  - `createWildcardType(typeNode, ..., extraAnnotations, memberAnnotations)` replaces two
    4-line copies of the wildcard construction.
  - `createClassifierOrPrimitive(typeNode, ..., extraAnnotations, memberAnnotations)`
    handles the primitive / `JAVA_CODE_REFERENCE` branches at the bottom.
  `createJavaType` itself is now ~25 lines and reads top-to-bottom as a dispatch chain.
- **O8** (`JavaMemberOverAst.kt:34-37, 322-326`) — promoted the base class's `modifierList`
  getter from `private` to `protected` (backing field `_baseModifierList` stays `private`)
  and deleted `JavaMethodOverAst._methodModifierList` plus its override. One fewer
  `@Volatile` field per method instance; the base-class cache is reused.
- **O9** (`JavaLightTree.kt`; three call sites) — deleted the `String` overloads of
  `findChildByType` and `getChildrenByType`. The three `"DOC_COMMENT"` call sites
  (`JavaClassOverAst.kt:382`, `JavaMemberOverAst.kt:69, 441`) now go through the shared
  helper (R3) which uses the typed constant `JavaDocSyntaxElementType.DOC_COMMENT`.
  Updated five test files that still used the String overload for other KMP node types
  (`CLASS`, `METHOD`, `FIELD`, `TYPE`, `IDENTIFIER`, `PACKAGE_STATEMENT`, `JAVA_CODE_REFERENCE`,
  `TYPE_PARAMETER_LIST`, `EXTENDS_BOUND_LIST`) to use
  `JavaSyntaxElementType.*` / `JavaSyntaxTokenType.*` directly. Removes the per-child
  `SyntaxElementType.toString()` allocation and one code path in the tree accessor layer.
- **R3** (`utils.kt:14-24`) — added `internal fun isDeprecatedInJavaDoc(tree, node):
  Boolean` with the canonical implementation. The three duplicate `override val
  isDeprecatedInJavaDoc` bodies in `JavaClassOverAst`, `JavaMemberOverAst` (members and
  value parameters) are now one-liners delegating to the helper.
  `JavaPackageOverAst.isDeprecatedInJavaDoc` (always `false`) is intentionally left as-is —
  it doesn't touch the AST and doesn't need the helper.

### Files Modified

| File | Items |
|------|-------|
| `JavaTypeOverAst.kt` | P1 (cache `typePositionAnnotations`); P7+R6 (extract `tryCreateArrayOrVarargFromTypeNode` / `createWildcardType` / `createClassifierOrPrimitive`) |
| `JavaMemberOverAst.kt` | O8 (drop `_methodModifierList`, promote base `modifierList` to `protected`); O9+R3 (three call sites delegate to `isDeprecatedInJavaDoc` helper) |
| `JavaClassOverAst.kt` | O9+R3 (delegate to `isDeprecatedInJavaDoc` helper) |
| `JavaLightTree.kt` | O9 (delete String overloads of `findChildByType` / `getChildrenByType`) |
| `utils.kt` | R3 (add `isDeprecatedInJavaDoc(tree, node)` helper) |
| `JavaParsingBasicTest.kt`, `JavaParsingModifiersAndSpecialClassesTest.kt`, `JavaParsingTypeResolutionTest.kt`, `JavaParsingTypeSystemTest.kt`, `JavaParsingAnnotationsTest.kt` | O9 follow-up: switched remaining String-overload calls to typed constants |

### Test Results

Gate per `REFACTORING_PLAN.md` §2.1 / Phase A baseline:
```
./gradlew :kotlin-java-direct:test \
  --tests JavaUsingAstPhasedTestGenerated \
  --tests JavaUsingAstBoxTestGenerated \
  --tests JavaParsingTest \
  --rerun-tasks
```
→ **BUILD SUCCESSFUL**, `tests=2671 failures=0 errors=0 skipped=0`. Identical count and
outcome to the Phase A baseline. IDE `get_file_problems` on every edited source and test
file reports 0 errors (pre-existing style warnings only).

### Key Learnings

- **Deleting a typed-API convenience overload has a blast radius beyond the main module.**
  The three `"DOC_COMMENT"` usages were the reason the `String` overloads existed, but
  five test files had come to depend on them too — for unrelated node types (CLASS,
  METHOD, FIELD, …). Removing the overload surfaced those dependencies as compile errors
  that Phase A's more conservative review hadn't flagged. In retrospect, any "delete the
  convenience overload" refactor should be preceded by a repo-wide grep of
  `, "[A-Z_]+")` to size the cleanup budget correctly.
- **Visibility promotion is a better fix than a redundant subclass cache.** The
  `JavaMethodOverAst._methodModifierList` duplication existed purely because the base
  class's `modifierList` was `private`. Promoting the *getter* to `protected` (the backing
  field can stay `private`) is strictly better than keeping parallel caches — the two
  caches would eventually drift if the definition of "modifier list" changed, and the
  per-method memory cost is real (tens of thousands of `JavaMethodOverAst` instances per
  large compilation).
- **Symmetry at the helper boundary beats two ad-hoc branches.** `createJavaType`'s two
  paths (TYPE-input vs. derived-`typeNode`) had identical structure but slightly different
  behaviour: Path 1 did the vararg annotation split, Path 2 didn't. Extracting the
  behaviour into a single helper forces the question "are these actually the same?" —
  and in this case they *are*, because Path 2's callers never pass a vararg node in the
  first place (fields can't be vararg). The non-applicable branch becomes a no-op instead
  of dead code.
- **`filterTypeUseAnnotations` is a good test case for "cache the unfiltered slice, filter
  the variable part".** The method's contract splits cleanly along "always include"
  (type-position annotations) and "maybe include" (member annotations with callback). P1
  makes that split visible in the code: the unfiltered slice is a property with a name,
  the filtered member set is computed inline where the callback enters. Same output,
  easier to read.

---

## Refactoring Plan Phase A: Correctness + Trivial Perf Cleanups — 2026-04-21

### Overview

First phase of `REFACTORING_PLAN.md`. Fourteen low-risk, localised edits landed in two
iterations: A.1 (correctness + dead code) and A.2 (trivial perf cleanups). Each item
corresponds to a numbered problem in the plan's §1 table. No measurements were required —
these are either correctness fixes or obvious micro-wins whose ROI is clear from reading
the code. Risky or speculative perf changes are deferred to Phase C (measure) / Phase D
(implement if measured positive).

### A.1 — Correctness and dead-code fixes

- **C1** (`JavaMemberOverAst.kt:203`) — `isInitializerPotentiallyConstant` compared
  `tree.getType(child).toString() != "NULL_LITERAL"`, but the actual parser token is
  `JavaSyntaxTokenType.NULL_KEYWORD` whose `toString()` returns `"NULL_KEYWORD"`. The check
  therefore never matched a null literal, so `hasConstantNotNullInitializer` would silently
  misreport `final Object x = null` as a potentially-constant non-null initializer. Replaced
  with the typed `tree.getType(child) != JavaSyntaxTokenType.NULL_KEYWORD`, which also drops
  a per-call `String` allocation.
- **C2 + O5 + R13** (`JavaResolutionContext.kt:45-54`) — the single-element
  `Array<Map<...>?>(1)` used as a mutable holder for aggregated inherited inner classes had
  no `@Volatile` guarantee on the slot; one thread's `holder[0] = result` could be unseen
  by another, causing either re-computation or a stale read under concurrent FIR resolution.
  Replaced with a small internal `class AggregatedInheritedInnerClassesHolder` carrying
  `@Volatile var value: Map<...>?`. Removed the bogus `@Suppress("ArrayInDataClass")`
  (`JavaResolutionContext` is not a data class).
- **C3** (`JavaClassFinderOverAstImpl.kt:285`) — `findPackage(fqName)` returned `null` whenever
  the per-package class map was empty, even if `package-info.java` had indexed
  package-level annotations. Now also consults `packageAnnotationNodes[fqName]` before
  bailing, so annotation-only packages become visible to FIR.
- **C4** (`ConstantEvaluator.kt:256-272`) — the sibling-class fallback in
  `ConstantEvaluator.findLocalClass` constructed a fresh `JavaClassOverAst` from the AST,
  bypassing the resolution context's class cache. That both defeated caching and broke the
  object-identity invariant FIR relies on for type-parameter matching (same invariant
  `localClassCache` is there to guarantee — see the 2026-04-20 entry). Routed through
  `containingClass.resolutionContext.findLocalClass(...)` instead; the local block
  shrinks from 20 lines to 2.
- **O1** (`JavaLightTree.kt:56, 264, 449`) — deleted the `startForDone: IntArray` field
  and its Pass-1 writes. It was `@Suppress("unused")` and unreferenced anywhere in the
  module, so it was pure waste: `markerCount × 4` bytes per tree plus the Pass-1 stores.
- **O4** (`JavaClassFinderOverAstImpl.kt:97`) — replaced the outdated
  `Collections.newSetFromMap(ConcurrentHashMap())` idiom on `negativeClassCache` with
  `ConcurrentHashMap.newKeySet()`, which is the direct API for this use case.

### A.2 — Trivial perf cleanups

- **P2** (`JavaMemberOverAst.kt:103-118`) — rewrote `computeLeadingFieldNode` as a reverse
  `for` loop. The previous `(myIndex - 1 downTo 0).map { siblings[it] }.firstOrNull { ... }`
  allocated an intermediate `List<JavaLightNode>` per call.
- **P3** (`JavaResolutionContext.kt:474-486`) — `resolveAsClassId` now maps the `FqName`
  path segments to `List<String>` once before the loop, instead of re-mapping
  `parts.subList(...).map { it.asString() }` on every iteration.
- **P4** (`JavaMemberOverAst.kt:170-178`) — cached `initializerNode` via `cachedNullable` +
  `NOT_COMPUTED` sentinel. It was previously recomputed from `hasConstantNotNullInitializer`
  and again from `initializerValue` / `resolveInitializerValue`.
- **P5** (`JavaMemberOverAst.kt:353-355`) — `hasAnnotationParameterDefaultValue` now
  short-circuits to a `DEFAULT_KEYWORD` presence check on the method node. The previous
  delegation to `annotationParameterDefaultValue != null` fully constructed a
  `JavaAnnotationArgument` for every probe just to throw it away.
- **P6** (`JavaTypeOverAst.kt:326-330`) — `JavaClassifierTypeOverAst.isResolved` collapsed
  to `classifier != null || resolutionContext.getSimpleImport(rawTypeNameParts[0]) != null`.
  The previous body re-called `findTypeParameter` even though `classifier` already consults
  it; the old fall-through branches contributed nothing to the outcome.
- **P10** (`JavaSupertypeGraph.kt:141-146`) — `getInnerClassNames` uses
  `mapTo(HashSet(size))` on the cached-class fast path with an `isEmpty()` short-circuit
  to `emptySet()`, instead of `.map {...}.toSet()`.
- **P11** (`JavaClassFinderOverAstImpl.kt:297-301`) — `knownClassNamesInPackage` now uses
  `buildSet { ... }` to build the result directly, avoiding the intermediate list and
  `.toSet()` copy.
- **C5** (`JavaSourceIndex.kt:103-105`) — switched the lightweight top-level declaration
  scanner from `DECLARATION_REGEX.find(effective)` to `findAll(effective)`, so multiple
  declarations on a single line (e.g. `class A {} class B {}`) are all indexed. Low-impact
  in real code but removes an edge-case correctness gap.

### Files Modified

| File | Change |
|------|--------|
| `JavaMemberOverAst.kt` | C1 (NULL_KEYWORD typed check), P2 (reverse for loop), P4 (`@Volatile` cache for `initializerNode`), P5 (`hasAnnotationParameterDefaultValue` is a keyword check) |
| `JavaResolutionContext.kt` | C2+O5+R13 (`AggregatedInheritedInnerClassesHolder` replaces `Array<...>(1)` + stray `@Suppress`), P3 (pre-map `parts` in `resolveAsClassId`) |
| `JavaClassFinderOverAstImpl.kt` | C3 (package annotations included in `findPackage`), O4 (`ConcurrentHashMap.newKeySet()`), P11 (`buildSet` in `knownClassNamesInPackage`) |
| `JavaLightTree.kt` | O1 (dropped `startForDone` field, constructor arg, and Pass-1 writes) |
| `ConstantEvaluator.kt` | C4 (routed `findLocalClass` through the resolution context's cache) |
| `JavaTypeOverAst.kt` | P6 (`JavaClassifierTypeOverAst.isResolved` collapsed to two disjuncts) |
| `JavaSupertypeGraph.kt` | P10 (`mapTo(HashSet(size))` + empty short-circuit in `getInnerClassNames`) |
| `JavaSourceIndex.kt` | C5 (`DECLARATION_REGEX.findAll` instead of `find`) |

### Test Results

Gate per `REFACTORING_PLAN.md` §2.1:
```
./gradlew :kotlin-java-direct:test \
  --tests JavaUsingAstPhasedTestGenerated \
  --tests JavaUsingAstBoxTestGenerated \
  --tests JavaParsingTest \
  --rerun-tasks
```
→ **BUILD SUCCESSFUL**, `tests=2671 failures=0 errors=0 skipped=0`. No diagnostic diffs
in the phased suite; no box regressions. IDE `build_project` on all 8 edited files
reports 0 errors (pre-existing style warnings only).

### Key Learnings

- **Token `toString()` is a name, not a literal to match.** `"NULL_KEYWORD"` is the
  `SyntaxElementType` constructor's debug label, and changing it on the `java-syntax`
  side would silently break any `.toString()`-based caller. The C1 bug is exactly the
  class of problem captured by `feedback_java_syntax_tokens.md`: always compare against
  the typed constant, never the string form.
- **`Array<T?>(1)` is not a substitute for `@Volatile`.** The array reference itself is
  a `final` field and therefore safely published, but stores into `array[0]` have no
  release semantics — other threads may observe the null-initialized slot indefinitely.
  A one-field class with `@Volatile var value` is both clearer and correct; it also makes
  the happens-before contract legible at the declaration site.
- **`package-info.java`-only packages are a real shape.** Several JDK and Kotlin-stdlib
  packages carry only annotations (`@kotlin.Metadata`, `@ParametersAreNonnullByDefault`,
  etc.) without any class files. Any "package exists?" check that looks only at class
  presence will lose those annotations — which is exactly what FIR's nullability pipeline
  would need. Keep the two sources (classes, annotations) symmetric in every existence
  predicate.
- **Helper delegation preserves cache identity.** The `ConstantEvaluator.findLocalClass`
  fix is a reminder that once a shared cache exists (`localClassCache` in the resolution
  context), every code path that could create the cached object must go through that
  cache. A freshly constructed `JavaClassOverAst` is not equal-by-identity to the cached
  one, and FIR's type-parameter matching is by identity — exactly the failure mode the
  2026-04-20 "localClassCache under concurrency" entry describes.
- **Short-circuits on the cheap check come first.** P5 (`DEFAULT_KEYWORD` presence before
  constructing the argument) and P6 (`classifier != null` before `getSimpleImport`) are
  the same idea applied twice: a `Boolean` property that forwards to a heavy computation
  is a smell; the presence/absence signal is almost always available from a cheaper
  source.

---

## Lazy Per-Package Indexing — 2026-04-21

### Overview

`buildIndex()` was the single most expensive method in java-direct: it ran in `init {}` and
eagerly walked **every** `.java` file across all source roots. For `testFrontend` (141 Java
files), profiling measured `buildIndex` at **156ms CPU** — roughly **40%** of java-direct's
total 389ms. PSI-based class finders skip this entirely because the IDE has pre-built indexes.

Replaced eager full-tree walking with **lazy per-package indexing**: the finder navigates to
the directory corresponding to a package on demand using `VirtualFile.findChild()` chains and
indexes only that directory's `.java` files. Packages never queried by the compiler are never
scanned.

Design doc: `implDocs/LAZY_PACKAGE_INDEXING_PLAN.md`.

### Changes

**Removed `buildIndex()`** (`JavaClassFinderOverAstImpl.kt`): The `init {}` block no longer
walks all files. Instead, source roots are classified: directory roots (production) are left
for lazy indexing; file-type roots (rare test-only edge case) are indexed eagerly into a
separate `fileRootIndex`.

**Lazy per-package indexing**: Three new methods form the core:
- `findPackageDirectories(fqName)` — navigates from each source root to the package directory
  via `findChild` chains (e.g., `root/"com"/"example"` for `com.example`). Results cached in
  a `ConcurrentHashMap`.
- `ensurePackageIndexed(fqName)` — `index.computeIfAbsent` that calls
  `indexPackageFromDirectories` and merges with `fileRootIndex` entries. Atomic: each package
  indexed at most once.
- `indexPackageFromDirectories(fqName)` — lists `.java` files in the package directory, applies
  existing size-based strategy (full parse ≤4096 bytes, lightweight scan otherwise). Validates
  that each file's declared package matches the directory-derived package (mismatched files
  skipped, matching javac behavior).

**All public API methods now call `ensurePackageIndexed`**: `isClassInIndex`, `findClass`,
`findClasses`, `findPackage`, `knownClassNamesInPackage`, `classesInPackage`,
`getPackageAnnotations`. Also the `sameClassInSameFilePackage` and `findFilesForClass`
callbacks used by `JavaSupertypeGraph`.

**`subPackagesOf` rewritten**: Uses directory listing directly (via `VirtualFile.children`)
instead of iterating all index keys with `asString().startsWith(prefix)` matching. Simpler
and O(children_in_directory) instead of O(total_packages × string_length).

**Thread safety**: `index` type changed from `HashMap` to
`ConcurrentHashMap<FqName, Map<String, List<FileEntry>>>` with immutable inner maps built
atomically inside `computeIfAbsent`. `packageAnnotationNodes` upgraded to `ConcurrentHashMap`
with `merge` for thread-safe concurrent writes from different packages.

**Package validation on `tryBuildFileEntry`**: `tryBuildFileEntry`, `tryBuildFileEntryWithFullParse`,
`tryBuildFileEntryLightweight`, and `indexPackageInfo` accept an optional `expectedPackage`
parameter. When non-null, files whose declared package doesn't match the directory are skipped.

### Files Modified

| File | Change |
|------|--------|
| `JavaClassFinderOverAstImpl.kt` | Replaced `buildIndex()` with lazy per-package indexing; added `findPackageDirectories`, `ensurePackageIndexed`, `indexPackageFromDirectories`; all API methods call `ensurePackageIndexed`; `subPackagesOf` rewritten to use directory listing; `index` → `ConcurrentHashMap`; `packageAnnotationNodes` → `ConcurrentHashMap`; `tryBuildFileEntry`/`indexPackageInfo` gain `expectedPackage` validation |

### Test Results

- `./gradlew :kotlin-java-direct:test` — **BUILD SUCCESSFUL**, 2769 tests, 0 failures, 0 errors.

### Key Learnings

- `VirtualFile.findChild()` is O(1) on VFS-cached directories (`CoreLocalVirtualFile` caches
  `File.listFiles()` results). Navigating a 5-segment package path costs 5 cached lookups —
  far cheaper than scanning all files in all directories to build a global index.
- `ConcurrentHashMap.computeIfAbsent` is the right primitive for lazy per-entry initialization:
  it is atomic per key, blocks concurrent callers for the same key, and establishes
  happens-before for subsequent reads. No separate "indexed" flag set is needed.
- `subPackagesOf` was O(total_packages) with string prefix matching — a hidden quadratic cost
  for deep package hierarchies. Replacing it with a single directory listing is both simpler
  and faster.
- File-type source roots (passing a `.java` file instead of a directory as a source root) are
  a test-only pattern. Handling them separately in init (via `fileRootIndex`) avoids
  complicating the main lazy-indexing path while preserving test compatibility.
- Validating declared package against directory structure (skipping mismatched files) matches
  javac behavior and avoids subtle indexing bugs where a file found in directory A would be
  indexed under package B.

---

## Precomputed Children + Types + Whitespace Exclusion — 2026-04-21

### Overview

The CHM-based children cache from the previous entry was tested on CI and showed no improvement
(slightly worse). Investigation with `ThreadMXBean.currentThreadCpuTime` counters revealed two
things:

1. **java-direct is only ~6% of pipeline CPU** (`testFrontend`: ~392ms java-direct vs ~6.9s
   total). So even large relative improvements in java-direct produce small absolute pipeline
   improvements that are within measurement noise.
2. **`getType()` was called 3.27M times** for a single module — the hottest method by far.
   Most calls came from linear scans inside `findChildByType` (111K calls × avg 29 children)
   and `getChildrenByType` (33K calls). Whitespace tokens, which are NEVER matched positively
   by any caller, accounted for ~44% of these `getType()` calls.

### Changes

**Precomputed children, types, offsets, and error flags** (`JavaLightTree.kt`): Replaced the
`ConcurrentHashMap`-based children cache with eager precomputation during `buildJavaLightTree`.
Pass 1 now also extracts `compositeTypes[]`, `compositeStartOffsets[]`, `compositeEndOffsets[]`,
and `errorFlags[]` alongside the existing parent/done indices. Pass 3 (new) builds children for
every composite node into `IntArray`-backed `ChildrenList` instances. All accessors —
`getChildren()`, `getType()`, `getStartOffset()`, `getEndOffset()` — are now plain array lookups
with no hashing, no marker-pool dispatch, and no `ConcurrentHashMap` overhead.

Memory cost: ~30 KB per file (vs ~1 MB for old `JavaSyntaxNode` tree) — still **97% reduction**.

**Whitespace exclusion from children**: `WHITE_SPACE` tokens are filtered out during children
construction (Pass 3). Every caller that previously did
`tree.getChildren(n).filter { tree.getType(it) != WHITE_SPACE }` now gets whitespace-free
children directly. This eliminated **1.44M `getType()` calls** (44% reduction) for the
`testFrontend` module.

**Caller cleanup** (6 files): Removed 15+ manual `filter { getType != WHITE_SPACE }` patterns
and unused `SyntaxTokenTypes` imports across `ConstantEvaluator.kt`, `JavaAnnotationOverAst.kt`,
`JavaClassOverAst.kt`, `JavaImportResolver.kt`, `JavaMemberOverAst.kt`, `JavaTypeOverAst.kt`.
Simplified `computeIsAnnotationType()` — no longer needs whitespace-skip loop since AT is
directly adjacent to INTERFACE_KEYWORD in whitespace-free children.

### Profiling Data (`testFrontend`, 141 Java files)

| Metric | Before (CHM cache) | After (precomputed + WS exclusion) | Delta |
|--------|-------------------|------------------------------------|-------|
| `getType()` calls | 3,269,891 | 1,825,500 | **–44%** |
| `getChildren()` calls | 222,506 | 222,506 | same |
| `findChildByType` calls | 111,454 | 111,454 | same |
| Children list sizes | ~k (incl. whitespace) | ~k/2 (WS excluded) | **–30-50%** |
| `buildLightTree` CPU | 75ms | 95ms | +20ms (WS filtering + precomputation) |
| `buildIndex` CPU | 172ms | 156ms | –16ms |
| `findClass` CPU | 145ms | 138ms | –7ms |
| java-direct total CPU | ~392ms | ~389ms | –3ms measured |
| java-direct fraction of pipeline | ~6% | ~6% | — |

### Files Modified

| File | Change |
|------|--------|
| `JavaLightTree.kt` | Major rewrite: precomputed `compositeTypes[]`, `compositeStartOffsets[]`, `compositeEndOffsets[]`, `errorFlags[]`, `childrenByIndex[]`; `ChildrenList` IntArray-backed wrapper; Pass 3 children construction with whitespace exclusion; removed CHM cache, `forEachDirectChild`, `scanTokensFor` |
| `ConstantEvaluator.kt` | Removed 3× `filter { getType != WHITE_SPACE }` + unused import |
| `JavaAnnotationOverAst.kt` | Removed 2× whitespace filter + unused import |
| `JavaClassOverAst.kt` | Simplified `computeIsAnnotationType` (no WS skip loop) + unused import |
| `JavaImportResolver.kt` | Removed 2× whitespace filter from fragmented-import recovery |
| `JavaMemberOverAst.kt` | Removed 5× whitespace filter + unused import |
| `JavaTypeOverAst.kt` | Removed 1× whitespace filter from `upperBounds` + unused import |

### Test Results

- `./gradlew :kotlin-java-direct:test` — **BUILD SUCCESSFUL**, 2769 tests, 0 failures, 0 errors.

### Key Learnings

- `ConcurrentHashMap` per-lookup overhead (hashing, volatile reads, key boxing) can negate caching
  benefits when the cached values are cheap to compute. Array-indexed precomputation avoids all
  three costs: `children[idx]` is a single memory load vs. CHM's hash→bucket→compare→return chain.
- Whitespace tokens were the single largest waste in `getType()` call volume. They comprised
  ~30-50% of children lists but were **never** matched positively — every caller either ignored
  them or explicitly filtered them. Excluding them at construction time is a one-time O(n) cost
  that pays off across all subsequent O(n) scans.
- `ThreadMXBean.currentThreadCpuTime` is essential for profiling in a Gradle test context: it
  measures only the calling thread's CPU, excluding system load, GC pauses, and Gradle overhead
  that make `System.nanoTime()` unreliable.
- java-direct accounts for only ~6% of full-pipeline CPU. Accessor-level optimizations (children
  caching, type precomputation) can improve that 6% but cannot close the 20-25% gap vs. PSI,
  which is dominated by `buildIndex` (eager file scanning that PSI skips entirely via pre-built
  IDE indexes) and per-file parsing cost.

---

## LightTree `getChildren()` Caching Fix (first attempt) — 2026-04-21

### Overview

The LightTree migration (Phases 1–4, April 20) replaced the materialized `JavaSyntaxNode` tree
with a flat-array `JavaLightTree`. While this eliminated ~130 MB of static AST memory, it
introduced a severe runtime regression: `getChildren()` allocated a fresh `ArrayList` and walked
the marker range **on every call**. In the old `JavaSyntaxNode`, `children` was a pre-built field —
zero-cost access. Since `findChildByType`, `getChildrenByType`, and `hasChildOfType` all delegated
to `getChildren()`, a single model object's cached properties collectively called `getChildren()` on
the same node 10–20 times, each rebuilding the list from scratch. The migration plan (§1.3, §5.3)
identified both a children cache and direct-scan `findChildByType` as planned mitigations but
neither was implemented.

Full analysis in `implDocs/archive/LIGHTTREE_PERFORMANCE_INVESTIGATION.md`.

### Changes

**Children cache** (`JavaLightTree.kt`): Added a `ConcurrentHashMap<Int, List<JavaLightNode>>`
that memoizes `getChildren()` results per node index. The tree is immutable, so repeated calls
return the same `List` instance. Only composite nodes that are actually queried enter the cache.

**`forEachDirectChild` inline helper**: Extracted the marker/token walking loop from the old
`getChildren` into a shared `inline fun` that both `computeChildrenImpl` (list building) and
direct-scan methods use, avoiding code duplication.

**Direct-scan `findChildByType`**: On cache miss, walks markers directly and returns on first
match — no list allocation. On cache hit, scans the cached list. Same for the `String`-typed
overload.

**Direct-scan `getChildrenByType`**: On cache miss, walks markers and collects only matching
children. On cache hit, filters the cached list. Same for the `String`-typed overload.

`hasChildOfType` is unchanged (delegates to `findChildByType`) but benefits from the optimization.

### Files Modified

| File | Change |
|------|--------|
| `JavaLightTree.kt` | +83/–30 lines: `childrenCache` CHM, `forEachDirectChild` inline helper, `scanTokensFor` inline helper, `computeChildrenImpl` extracted from `getChildren`, `findChildByType` / `getChildrenByType` rewritten with cached fast path + direct-scan slow path |

### Test Results

- `./gradlew :kotlin-java-direct:test` — **BUILD SUCCESSFUL**, 2769 tests, 0 failures, 0 errors,
  0 skipped, across 463 XML reports.

### Benchmark Results

`KotlinFullPipelineTestsGenerated` (414 modules), sequential mode (`SAME_THREAD`):

| Run | Total time | Delta |
|-----|-----------|-------|
| Without fix (LightTree, no children cache) | 255.5s | baseline |
| With fix (LightTree + children cache + direct-scan) | 242.2s | **–13.2s (–5.2%)** |

The 5.2% full-pipeline improvement corresponds to roughly 20–25% improvement in java-direct's own
execution time (java-direct accounts for ~20–25% of the pipeline). Concurrent mode has ~10%
variance, making small improvements undetectable in that mode.

### Key Learnings

- `getChildren()` was called ~184 times across 11 source files, and every `findChildByType`,
  `getChildrenByType`, and `hasChildOfType` call went through it. The cumulative effect was
  ~1.2M redundant ArrayList allocations per project — each walking the marker range and boxing
  `JavaLightNode` value class instances.
- The migration plan correctly identified caching and direct-scan as needed optimizations (§1.3,
  §5.3) but they were deferred and forgotten during the multi-phase migration. Future migrations
  should treat plan-identified optimizations as mandatory steps.
- A flat-array representation (LightTree) is only faster than a materialized tree **if on-demand
  access is amortized**. Without caching, the "no per-node overhead" claim traded static memory
  savings for dynamic allocation pressure on the hot path — a net regression.

---

## Remaining PERFORMANCE_REVIEW Work: resolve() Dedup, CHM Downgrades, Cache-Helper Unification — 2026-04-20

### Overview

Closes the remaining medium-value items from `implDocs/PERFORMANCE_REVIEW.md` (§2 items 6 & 7,
§4 overkill `ConcurrentHashMap` instances) and unifies the hand-rolled `@Volatile` caching
pattern that proliferated across the model layer during the two previous optimization rounds.
One item (§2 #6, context-level `tryResolve` cache) is intentionally deferred with a recorded
correctness argument. One related line of investigation (Kotlin 2.4's `ExplicitBackingFields`
language feature) produced a negative result.

### Change 1 — Deduplicate `resolve*` / `resolve*WithoutInheritance` (§2 #7)

**Problem**: `JavaResolutionContext` carried two near-identical pairs of resolution methods:
- `resolveSimpleNameToClassId` (full rules: imports, local/inner classes, inherited inner
  classes, same-package, `java.lang`, star imports with ambiguity + class-level handling)
  versus `resolveSimpleNameToClassIdWithoutInheritance` (a strict subset: imports,
  same-package, `java.lang`, simple star imports).
- `resolveNestedClassToClassIdFromParts` versus
  `resolveNestedClassToClassIdFromPartsWithoutInheritance` with the same structural
  difference.

The duplication was ~90 lines of Kotlin that had to stay in lock-step; any fix to one risked
silent divergence from the other.

**Fix** (`JavaResolutionContext.kt`): Collapsed each pair into a single `*Impl` workhorse with
a `checkInheritance: Boolean` parameter. When `false`, the impl skips:
- step 2 (`findLocalClass`) and step 2b (aggregated inherited inner classes / BFS fallback)
  in the simple-name resolver,
- the `findInheritedNestedClass` probe inside the nested resolver,
- the class-finder-based inherited-inner-class fallback for `size == 2` names,
- the class-level star-import handling and cross-star-package ambiguity check in step 5.

The explicit-import step behaves differently in the two flavors (`resolveAsClassId` vs.
`ClassId.topLevel`) and the flag gates that too — a difference that was easy to miss in the
old copy-pair. Thin public wrappers (`resolveSimpleNameToClassId`, `resolveNestedClassToClassId`)
pass `checkInheritance = true`; the `WithoutInheritance` callers now call the impl directly
with `checkInheritance = false`.

### Change 2 — `ConcurrentHashMap` → `HashMap` for build-once-read-many maps (§4)

**Problem**: Three maps in `JavaClassFinderOverAstImpl` were `ConcurrentHashMap` despite being
populated only inside the single-threaded `init{}` block and never mutated afterward:
- `index: MutableMap<FqName, MutableMap<String, MutableList<FileEntry>>>` (outer + inner maps)
- `packageAnnotationNodes: MutableMap<FqName, MutableList<JavaAnnotation>>`

The concurrent-hash overhead was pure waste: after `buildIndex()` returns, the maps become
effectively immutable. Publication to subsequent readers is already guaranteed by
`init{}` + `final` fields.

**Fix** (`JavaClassFinderOverAstImpl.kt`): Downgraded both to plain `HashMap`. Added comments
spelling out the "populated only in `init{}`, read-only afterwards" invariant so that any
future code that mutates these post-`init` will be caught in review.

Six other maps (`classCache`, `packageCache`, `negativeClassCache`, `supertypeCache`,
`inheritedInnerClassesCache`, `innerClassCache`) stay `ConcurrentHashMap` — they're genuinely
written during concurrent FIR resolution.

### Change 3 — Unified cache helpers (`CacheHelpers.kt`)

**Problem**: Three `@Volatile`-backed lazy-cache patterns had proliferated across the model
layer after `by lazy(PUBLICATION)` was eliminated:
1. Non-null cache (35+ sites): `@Volatile private var _x: T? = null` + 6-line getter.
2. Nullable cache with sentinel (10+ sites): `@Volatile private var _x: Any? = NOT_COMPUTED`
   + 7-line getter.
3. Tri-state `Int` for `Boolean` (8+ sites): `@Volatile private var _x: Int = -1` + 7-line getter.

The hand-rolled sequences were error-prone (two sites used a bespoke `CLASSIFIER_NOT_COMPUTED`
sentinel instead of the shared `NOT_COMPUTED`; three classes carried their own duplicate
`NOT_COMPUTED` companion objects).

**Fix** — new file `CacheHelpers.kt`, one module-level sentinel, three `internal inline`
functions:
- `cachedNonNull<T : Any>(read, write, compute): T`
- `cachedNullable<T>(read, write, compute): T` (uses the shared `NOT_COMPUTED`)
- `cachedBoolean(read, write, compute): Boolean` (tri-state `Int` backing)

Each helper is `inline`: at every call site, Kotlin expands the helper plus both lambdas into
bytecode byte-for-byte equivalent to the hand-rolled sequence. There is no runtime cost
compared to the previous per-property expansion — this is purely an API unification.

All ~45 cache sites across `JavaClassOverAst`, `JavaMemberOverAst`, `JavaTypeOverAst`, and
`JavaAnnotationOverAst` now read:
```kotlin
@Volatile private var _x: ...
val x: ... get() = cachedXxx({ _x }, { _x = it }) { <expensive> }
```
Three redundant per-class `NOT_COMPUTED` sentinels were deleted; the per-class
`CLASSIFIER_NOT_COMPUTED` in `JavaClassifierTypeOverAst` was merged into the shared one.

### Change 4 — `ExplicitBackingFields` is not usable (negative result)

The user asked whether Kotlin 2.4's `ExplicitBackingFields` feature could eliminate the
underscore-prefixed pattern entirely. Verified by a one-file probe that the 2.4 compiler:
1. **Requires the backing field's type to be a subtype of the property's type.** So a
   property typed `T` cannot have a backing field typed `T?`, and the lazy-cache pattern
   needs exactly that.
2. **Rejects properties that declare both an explicit `field` and a custom accessor.** But
   the lazy-cache pattern's whole purpose is a custom `get()` that consults the field.

Observed compiler errors (recorded for future reference):
```
e: The type of the backing field must be a subtype of the property's type.
e: Properties with explicit backing fields cannot have accessors.
e: Reassignment of read-only property via backing field.
```

The feature in its 2.4 form is designed for narrowing public types (e.g. `val flow: Flow<T>`
with `field: MutableSharedFlow<T>`), not for generic lazy caching. The probe was removed
after verification.

### Change 5 — Skipped: context-level `tryResolve` cache (§2 #6)

**Why skipped**: The current per-call `HashMap<ClassId, Boolean>` in `JavaResolutionContext.resolve`
is allocated only for dotted names (simple names bypass it). Hoisting it to the
context level would only be correct if FIR's `tryResolve` callback is deterministic for a
given `ClassId` across the resolution context's lifetime. In practice FIR *can* pass
different callbacks for different type references (different visibility scopes in principle),
and cache reuse keyed on `ClassId` alone would risk returning a stale yes/no answer under
a different callback's rules. The saving (a few `HashMap` allocations per file, only for
dotted names) doesn't justify the correctness risk without stronger guarantees about FIR's
callback contract. Re-evaluate if a future task strengthens that contract.

### Files Modified

| File | Change |
|------|--------|
| `CacheHelpers.kt` (new) | Shared `cachedNonNull` / `cachedNullable` / `cachedBoolean` inline helpers + module-level `NOT_COMPUTED` sentinel |
| `JavaClassOverAst.kt` | All 16 cache sites rewritten to use helpers; per-class `NOT_COMPUTED` companion deleted |
| `JavaMemberOverAst.kt` | All 15 cache sites rewritten; base-class `protected NOT_COMPUTED` companion and `JavaValueParameterOverAst`'s private sentinel both deleted |
| `JavaTypeOverAst.kt` | 10 cache sites rewritten; `CLASSIFIER_NOT_COMPUTED` folded into shared sentinel |
| `JavaAnnotationOverAst.kt` | 3 cache sites rewritten; per-class `NOT_COMPUTED` companion deleted |
| `JavaClassFinderOverAstImpl.kt` | `index` (outer + inner) and `packageAnnotationNodes` downgraded to `HashMap` |
| `JavaResolutionContext.kt` | `resolveSimpleNameToClassIdImpl` / `resolveNestedClassToClassIdFromParts` with `checkInheritance: Boolean` replace two pairs of near-duplicate methods |

### Test Results

- `./gradlew :kotlin-java-direct:test` via the `Tests in 'kotlin.kotlin-java-direct'` run
  configuration — **BUILD SUCCESSFUL**, `tests=2769 failures=0 errors=0 skipped=0` across
  463 XML reports. Matches the baseline from the previous iteration (same day).
- JetBrains IDE `build_project` on all 7 changed source files — 0 errors, no new warnings.

### Key Learnings

- Kotlin `inline fun` with lambda parameters is a legitimate zero-cost API-unification tool
  as long as the helper doesn't need to own the backing storage. The helper here takes the
  field accessors as lambdas, so every call site still declares its own `@Volatile` field —
  but the read-check-compute-store sequence is shared and consistent.
- `ExplicitBackingFields` (Kotlin 2.4) is narrower than its name suggests: it doesn't give
  you a general "expose the backing field" escape hatch, it gives you a *narrowed* backing
  field type with no custom getter. Knowing this upfront saves a speculative refactor.
- When two methods have an 80-line body and an 8-line body that is a strict subset, a
  single `checkInheritance` boolean parameter is usually cleaner than two separate
  implementations. The opposite smell (boolean parameters that gate unrelated behavior)
  doesn't apply here because all gated branches are on the same axis ("should this level of
  resolution recurse into inherited-member lookup?").
- `ConcurrentHashMap` is a useful hint in code review: "every write is concurrent with
  other writes/reads". Using it for populate-once-read-many maps loses that hint and costs a
  little performance. Prefer `HashMap` + an `init{}` publication invariant that is
  documented in a comment.

---

## Hot-Path Caching + Concurrency Correctness — 2026-04-20

### Overview

Re-audit of `PERFORMANCE_REVIEW.md` after the LightTree migration (see next iteration) showed that
the original review's items 5.1/5.2 (materialized AST vs. LightTree) were closed by the migration,
and items 1.1/2.1/2.2/2.3/3.4 were closed by the two April 17 performance rounds. A revised review
(`implDocs/PERFORMANCE_REVIEW.md`, old version moved to `implDocs/archive/PERFORMANCE_REVIEW.md`)
identified a short list of remaining quick wins and two thread-safety issues latent under the
module's "FIR resolution is concurrent" assumption. This iteration implements all of them in one
pass.

### Changes

**Caching (allocation reduction):**

- `JavaValueParameterOverAst` — `type`, `isVararg`, `modifierList`, `annotations` converted from
  plain `get()` to `@Volatile`-backed caches. Parameters are iterated repeatedly by FIR during
  overload resolution; previously every read re-walked the AST and re-allocated
  `JavaAnnotationOverAst` wrappers. Added a class-local `NOT_COMPUTED` sentinel because the class
  extends `JavaElementOverAst` (not `JavaMemberOverAst`, whose sentinel is `protected`).
- `JavaTypeParameterOverAst.annotations` — `@Volatile`-cached. Contract-compatible with the
  two-phase construction invariant: annotations don't reference sibling type parameters, so they
  can be cached after `updateResolutionContext` is called (same rule as `upperBounds`).
- `JavaAnnotationOverAst` — `annotationName`, `classId`, `arguments` cached. `annotationName` is
  read three times per annotation (from `classId`, `isResolved`, `resolveAnnotation`); one cache
  covers all three. Uses a `NOT_COMPUTED` sentinel for the nullable `classId`.
- `JavaTypeOverAst.annotations` — converted from `by lazy(PUBLICATION)` to the `@Volatile` pattern
  used by the rest of the module. Base-class delegate inherited by every `JavaType*OverAst`, so
  this drops one `SafePublicationLazyImpl` wrapper per type instance.

**Concurrency correctness (`getOrPut` → `computeIfAbsent` on `ConcurrentHashMap`):**

`getOrPut` on `ConcurrentHashMap` is non-atomic — two concurrent threads can both compute the value
and the second `put` silently discards the first's result. For deterministic computations the
outcome is the same but the wasted work is not free.

- `JavaSupertypeGraph.getDirectSupertypes` — re-parses files on the cold path; concurrent
  double-compute would mean concurrent double-parse.
- `JavaSupertypeGraph.collectInheritedInnerClasses` — recursive supertype walk. Inner recursive
  reads still use plain `get` (not `computeIfAbsent`) so the top-level `computeIfAbsent` cannot
  self-deadlock.
- `JavaClassFinderOverAstImpl.findPackage`.
- `JavaClassOverAst.findInnerClass` — kept the lock-free fast path via an initial `get`; only a
  miss enters `computeIfAbsent`.

**Thread-safety (plain `HashMap` → `ConcurrentHashMap`):**

Two plain `HashMap` caches were living on paths that every other cache in the module treats as
concurrent. Both were race-prone under FIR's concurrent resolution of members of the same class.

- `JavaScopeResolver.findLocalClassCache` — shared across scopes produced by `withTypeParameters`
  / `withInheritedTypeParameters`. Now `ConcurrentHashMap<Name, Any>` + `computeIfAbsent`, null
  results encoded via the existing `FIND_LOCAL_CLASS_NULL` sentinel.
- `JavaResolutionContext.Companion.create`'s `localClassCache` — shared across all users of the
  resolution context. Critical for `JavaClassOverAst` identity: FIR matches type parameters by
  object identity, so a race that produced two distinct `JavaClassOverAst` instances for the
  same name would surface as `ERROR CLASS: Unresolved name: T`. `computeIfAbsent` guarantees a
  single canonical instance per name.

### Files Modified

| File | Change |
|------|--------|
| `JavaMemberOverAst.kt` | `JavaValueParameterOverAst`: cache `type`, `isVararg`, `modifierList`, `annotations`; add class-local `NOT_COMPUTED` sentinel |
| `JavaTypeOverAst.kt` | `JavaTypeOverAst.annotations`: `by lazy` → `@Volatile`; `JavaTypeParameterOverAst.annotations`: add `@Volatile` cache |
| `JavaAnnotationOverAst.kt` | Cache `annotationName`, `classId`, `arguments`; add `NOT_COMPUTED` sentinel companion |
| `JavaSupertypeGraph.kt` | `getDirectSupertypes` / `collectInheritedInnerClasses`: `getOrPut` → `computeIfAbsent` |
| `JavaClassFinderOverAstImpl.kt` | `findPackage`: `getOrPut` → `computeIfAbsent` |
| `JavaClassOverAst.kt` | `findInnerClass`: retain fast-path `get`, use `computeIfAbsent` on miss |
| `JavaScopeResolver.kt` | `findLocalClassCache`: `HashMap` → `ConcurrentHashMap` with `computeIfAbsent` |
| `JavaResolutionContext.kt` | `localClassCache` inside `Companion.create`: `mutableMapOf` → `ConcurrentHashMap` + `computeIfAbsent` |
| `implDocs/PERFORMANCE_REVIEW.md` | Rewritten as the post-LT-migration review; old version moved to `implDocs/archive/` |

### Test Results

- `./gradlew :kotlin-java-direct:test` — **BUILD SUCCESSFUL**. 2769 tests, 0 failures, 0 errors,
  0 skipped, across 463 XML reports.
- JetBrains IDE `get_file_problems` — 0 errors in all 8 changed source files; remaining warnings
  are pre-existing "unstable API" references to `com.intellij.platform.syntax.*`, not introduced
  by this iteration.

### Key Learnings

- `getOrPut` on `ConcurrentHashMap` is not atomic — it compiles and behaves "correctly" under
  racy access but silently duplicates work. Every getOrPut call on a concurrent cache in this
  module was a candidate; `computeIfAbsent` is the atomic replacement and also clearer about
  intent. Keep a fast-path `get()` before `computeIfAbsent` if the hot path is lock-free reads
  (as in `findInnerClass`).
- The module enforces a specific threading contract (every shared cache is concurrent) but two
  `HashMap`s had slipped through. When a codebase commits to "concurrent everywhere", the cost
  of a single non-concurrent outlier is two-fold: it races under load and it teaches readers
  that "it's OK to use `HashMap` here sometimes".
- Caching `annotationName` was a one-line change with a 3× saving per annotation — the cheapest
  wins were still visible after two rounds of performance work. Worth re-auditing hot paths after
  any architectural change (like the LightTree migration) because new patterns expose new
  hot spots.

---

## LightTree Migration: Phases 1–4 — 2026-04-20

### Overview

The original performance review flagged the fully-materialized `JavaSyntaxNode` tree as the single
largest structural contributor to the gap vs. PSI: every token allocated a ~64-byte node plus an
`ArrayList` of children, on top of the source `CharSequence` being retained for every cached
class. PSI solved the equivalent problem via `LighterASTNode` — a flat-array representation where
a node is an `Int` index, not an object. This iteration migrates java-direct to the same pattern
over four phases, each gated on passing tests so that the migration could be reviewed and
committed incrementally.

Planning doc: `implDocs/LIGHTTREE_MIGRATION_PLAN.md`. Analysis: `implDocs/LIGHTTREE_MIGRATION_ANALYSIS.md`.

### Phase 1 — Infrastructure and unit-test harness

Commit: `d726603a2b75 ~ [cc] LT migration phase 1`.

Problem: the migration target (`JavaLightTree` wrapping `ProductionMarkerList` + `TokenList` from
`com.intellij.platform.syntax.parser`) did not exist yet; the existing `JavaParsing*Test` suites
could not be run against it until all model classes were migrated (Phase 3), so Phase 1 needed its
own unit-test harness.

Fix:
- New `JavaLightTree.kt` (~400 lines): `JavaLightNode(val index: Int)` inline value class,
  `JavaLightTree` holding precomputed `parentStartIndex[]` / `doneForStart[]` / `startForDone[]` /
  `tokenParentStart[]` lookup arrays, and `buildJavaLightTree(builder, source)` stack-based
  single-pass factory. API parallels the legacy `JavaSyntaxNode` extensions: `getRoot()`,
  `isComposite()` / `isToken()`, `getType()`, `getStartOffset()` / `getEndOffset()`, `getText()`,
  `textEquals()`, `getParent()`, `getChildren()`, `findChildByType()` / `getChildrenByType()` /
  `hasChildOfType()`.
- Synthetic root (`rootIndex = markerCount`) wraps all top-level productions so existing
  `root.children`-walking code paths continue to work. Error markers (no matching done-pair) are
  treated as leaves in `getEndOffset` / `getChildren`.
- New `JavaLightTreeTest.kt` (~280 lines, 18 tests) exercises primitives that the
  `JavaParsing*Test` suites don't cover: token `getParent`, `isToken` / `isComposite`,
  `textEquals` edges, malformed input tolerance, `dump()` output, synthetic root offsets.

Bumps and fixes during Phase 1:
- First implementation returned the first top-level `START` marker as root; legacy
  `buildSyntaxTree` synthesizes a wrapper root spanning `[0, source.length)`. 17/18 tests failed.
  Switched to `rootIndex = markerCount` sentinel.
- `testFindChildByTypeReturnsNullWhenAbsent` was testing the `EXTENDS_LIST` on `class A {}` which
  is actually *present* but empty; retargeted the test to `INTERFACE_KEYWORD` and `RECORD_HEADER`
  which are truly absent.
- `-Werror` complained about `listOf<...>(...) as List<SyntaxElementType>`; replaced with an
  explicit type parameter on `listOf<SyntaxElementType>(...)`.

### Phase 2 — Parallel verification

Commit: `059e0ae0c23b ~ [cc] LT migration phase 2`.

Problem: before migrating callers, we wanted to confirm that `JavaLightTree` produces the same
structure as `JavaSyntaxNode` across the full test corpus, not just the 18 unit tests from Phase 1.

Fix:
- `JavaParsingTestBase` extended to build both representations for every parsed source and
  compare: node types, offsets, text, parent/child structure.
- `testPublicClassWithMalformedMembers` immediately flagged an `IndexOutOfBoundsException: Index
  -1 out of bounds for length 37` in `getEndOffset` when error markers were reached:
  `doneForStart[errorMarkerIndex]` is `-1`. Added an `isErrorMarker` short-circuit so error
  markers return the marker's own `getEndOffset()` directly and have empty `getChildren()`.

### Phase 3 — Migrate model, resolution, and finder layers

Commit: `89aa4d884429 ~ [cc] LT migration phase 3 (main)` — 18 files, +792 / –842 lines.

Problem: all `JavaElementOverAst` / `JavaClassOverAst` / `JavaTypeOverAst` / `JavaMemberOverAst` /
`JavaAnnotationOverAst` / `JavaResolutionContext` / `JavaImportResolver` /
`JavaClassFinderOverAstImpl` / `JavaSupertypeGraph` still referenced `JavaSyntaxNode`. Direct
replacement: every `.findChildByType(X)` / `.children` / `.text` / `.type` / `.textEquals(...)` on
a `JavaSyntaxNode` becomes `tree.findChildByType(node, X)` / `tree.getChildren(node)` /
`tree.getText(node)` / `tree.getType(node)` / `tree.textEquals(node, ...)`.

Fix:
- `JavaElementOverAst` base takes `(val node: JavaLightNode, val tree: JavaLightTree)`; `equals`
  compares both.
- All per-file caches keyed on `JavaSyntaxNode` become keyed on `JavaLightTree`
  (e.g. `JavaImportResolver.importCache`).
- `JavaResolutionContext.create(tree, classFinderProvider)` factory takes a tree and calls
  `tree.getRoot()`; all previous callers pass `tree` in place of the old root.
- `JavaClassFinderOverAstImpl` paths (`indexPackageInfo`, `tryBuildFileEntryWithFullParse`,
  `parseTopLevelClassFromFile`) use `parseJavaToLightTree(source, 0)` + `tree.getRoot()`.
- `JavaParsingTestBase.parseSource()` returns a `ParsedSource(root, context, tree)` data class.
  Component order chosen so existing `val (root, context) = parseSource(...)` destructuring in
  the per-feature parsing tests continues to compile unchanged.
- `parse.kt` gained a `parseJavaToLightTree(charSequence, start)` wrapper around the KMP parser.

Test run after Phase 3: **2766 / 2766 pass** (98 unit + 1488 phased + 1180 box).

### Phase 4 — Remove legacy and assess the new test suite

Commit: `3f16b322018d ~ [cc] LT migration phase 4 (cleanup)`.

Problem: with all callers on `JavaLightTree`, the legacy `JavaSyntaxNode` class,
`buildSyntaxTree()`, and the parallel verification code in `JavaParsingTestBase` are dead weight.

Fix:
- `utils.kt` rewritten: the `JavaSyntaxNode` class and `buildSyntaxTree()` deleted (~150 LOC
  removed); only `computeTypeParameters(node, tree, resolutionContext)` remains.
- `JavaParsingTestBase`'s parallel-verification code stripped.
- Stale KDoc references to `JavaSyntaxNode` / `buildSyntaxTree` in `JavaLightTree.kt` and
  `JavaLightTreeTest.kt` rewritten to reference only the new API.

`JavaLightTreeTest` assessment — does it still earn its keep after migration?
- `JavaParsing*Test` exercises `JavaLightTree` transitively but through the model layer. It does
  not directly test primitives like token `getParent`, `isToken` / `isComposite`, `textEquals`
  edges, malformed-input tolerance, `dump()` output, or synthetic root offsets.
- Kept. Rationale recorded in the test file's KDoc: these are the `JavaLightTree` primitives that
  are not incidentally covered by the model-level tests, and regressions on them would surface
  as confusing failures far from the root cause.

### Files Modified

Beyond the file-level diffs above, the net shape of the change is:

| File | Phase | Change |
|------|-------|--------|
| `JavaLightTree.kt` (new) | 1, 2, 4 | Flat-array tree + API surface; error-marker handling; KDoc cleanup |
| `parse.kt` | 1 | `parseJavaToLightTree` wrapper |
| `JavaLightTreeTest.kt` (new) | 1, 4 | 18 unit tests for `JavaLightTree` primitives |
| `JavaElementOverAst.kt` | 3 | Base class takes `(node, tree)` |
| `JavaTypeOverAst.kt` | 3 | All type subclasses migrated |
| `JavaClassOverAst.kt` | 3 | Migrated |
| `JavaMemberOverAst.kt` | 3 | Field / method / constructor / parameter all migrated |
| `JavaAnnotationOverAst.kt` | 3 | All argument types migrated |
| `ConstantEvaluator.kt` | 3 | `tree` exposed via `containingClass.tree` |
| `JavaImportResolver.kt` | 3 | `WeakHashMap` keyed on `JavaLightTree` |
| `JavaResolutionContext.kt` | 3 | `create(tree, …)` factory; `extractImports(tree, root)` |
| `JavaSupertypeGraph.kt` | 3 | `extractSupertypeRefsFromNode(tree, classNode, …)` |
| `JavaClassFinderOverAstImpl.kt` | 3 | All parsing paths use `parseJavaToLightTree` |
| `JavaRecordComponentOverAst.kt` | 3 | Migrated |
| `utils.kt` | 4 | Rewritten down to `computeTypeParameters` only |
| `JavaParsingTestBase.kt` | 2, 3, 4 | Parallel verification; `ParsedSource` data class; cleanup |
| `JavaParsing*Test.kt` (5 files) | 3 | Raw AST navigation rewritten to `tree.*` |

### Test Results

Green at the end of every phase. Final Phase-4 run: `./gradlew :kotlin-java-direct:test` —
**BUILD SUCCESSFUL**, 2766 tests, 0 failures, 0 errors.

### Key Learnings

- Legacy behavior sometimes hides in invisible places. The `buildSyntaxTree` synthetic root is
  documented nowhere in the legacy class — the first `JavaLightTree` built without it failed 17
  out of 18 tests, most of which walked `root.children`. When replacing an established abstraction,
  reproduce its observable shape (here: a single root that spans `[0, source.length)`) even if
  the docs don't call the shape out.
- Error markers are the subtle cost of parser recovery. The KMP Java parser emits them for
  unbalanced or malformed input; they have no done-pair, so any traversal assuming every `START`
  has a matching `DONE` crashes. Short-circuit on error markers in `getEndOffset` and
  `getChildren` — don't try to synthesize a done for them.
- Preserving destructuring compatibility is cheaper than renaming call sites. `ParsedSource`'s
  component order (`root`, `context`, `tree`) was chosen specifically so `val (root, context) =
  parseSource(...)` in every existing per-feature test continued to compile. Saved a large
  low-value rewrite for tests the migration did not conceptually change.
- Parallel verification in Phase 2 catches classes of bugs that unit tests for the new code
  never see. The error-marker crash wasn't reachable from the 18 Phase-1 tests; it only
  surfaced when the comparison was run against the full parsing-test corpus.
- A dedicated unit-test suite (`JavaLightTreeTest`) stays useful after higher-level tests cover
  the same type transitively — primitive-level failures show up as primitive-level failures
  there, not as confusing errors deep in the model layer.

---

## Performance Optimizations: Allocation Reduction on Hot Paths — 2026-04-17

### Overview

Performance review identified that java-direct is ~20% slower than PSI-based approach on large pipeline
tests. Root causes include excessive lazy delegate overhead, redundant string splitting, uncached annotations,
and O(n²) string re-joining in resolution. This iteration implements 5 targeted quick-win optimizations.

Full analysis in `implDocs/PERFORMANCE_REVIEW.md`.

### Optimization 1 — Cache `rawTypeNameParts` in `JavaClassifierTypeOverAst`

**Problem**: `rawTypeName.split('.')` was called independently in 4 lazy properties (`classifier`,
`isTriviallyFlexibleHint`, `classifierQualifiedName`, `isResolved`), producing 4 array allocations
per type reference for the same string.

**Fix** (`JavaTypeOverAst.kt`): Added a `rawTypeNameParts` cached property that splits once. All 4
consumers now read the cached result.

### Optimization 2 — Cache `annotations` in `JavaTypeOverAst`

**Problem**: `annotations` was a computed `get()` property that created new `JavaAnnotationOverAst`
wrapper objects on every access. FIR accesses annotations multiple times per type (nullability,
deprecation, etc.), multiplying allocations.

**Fix** (`JavaTypeOverAst.kt`): Converted to `by lazy(PUBLICATION)` so annotation wrappers are
created once and reused.

### Optimization 3 — Pre-compute file basename in `FileEntry`

**Problem**: `knownClassNamesInPackage` called `entry.file.name.removeSuffix(".java")` per file entry
per iteration, creating a new string allocation each time.

**Fix** (`JavaClassFinderOverAstImpl.kt`): Added `fileBaseName` field to `FileEntry` data class with
default computed from `file.name`. The `knownClassNamesInPackage` filter now uses the pre-computed value.

### Optimization 4 — Eliminate `joinToString(".")` in recursive nested class resolution

**Problem**: `resolveNestedClassToClassId` recursively called itself with `outerParts.joinToString(".")`,
which the callee immediately re-split with `name.split('.')`. For a name like `"a.b.c.D.E"`, this
produced O(n²) string allocations across recursion levels. Same pattern in
`resolveNestedClassToClassIdWithoutInheritance`.

**Fix** (`JavaResolutionContext.kt`): Introduced `resolveNestedClassToClassIdFromParts` and
`resolveNestedClassToClassIdFromPartsWithoutInheritance` that accept `List<String>` directly.
The public entry points split once; recursive calls pass `subList` views (zero-copy) without
re-joining.

### Optimization 5 — Replace `by lazy(PUBLICATION)` with `@Volatile` on `JavaClassifierTypeOverAst`

**Problem**: `JavaClassifierTypeOverAst` is the most-instantiated model class (one per type reference,
200K+ in large projects). It had 8 `by lazy(PUBLICATION)` delegates, each allocating ~32 bytes
(`SafePublicationLazyImpl` wrapper + captured lambda) = ~256 bytes per instance of pure delegate
overhead, totalling ~50 MB for 200K instances.

**Fix** (`JavaTypeOverAst.kt`): Replaced all 8 lazy delegates with manual `@Volatile`-backed fields
using the same pattern as Step 3.6 for `JavaSyntaxNode`:
- `rawTypeName`, `rawTypeNameParts`: `@Volatile String?` / `@Volatile List<String>?`
- `classifier`: `@Volatile Any?` with `CLASSIFIER_NOT_COMPUTED` sentinel (null is a valid result)
- `isTriviallyFlexibleHint`, `isRaw`: `@Volatile Int` tri-state (-1/0/1)
- `classifierQualifiedName`: `@Volatile String?` (non-null result, null = not computed)
- `typeArguments`, `containingClassIds`: `@Volatile List<...>?`

Per-instance overhead drops from ~256 bytes (8 delegates) to ~72 bytes (9 volatile reference/int slots),
a ~184 bytes/instance saving. At 200K+ instances, this is ~36 MB of heap reduction.

### Files Modified

| File | Change |
|------|--------|
| `JavaTypeOverAst.kt` | Cached `rawTypeNameParts`; cached `annotations`; replaced 8 lazy delegates with `@Volatile` fields |
| `JavaClassFinderOverAstImpl.kt` | Pre-computed `fileBaseName` in `FileEntry`; used in `knownClassNamesInPackage` |
| `JavaResolutionContext.kt` | Split recursive resolution into `*FromParts` variants avoiding `joinToString` |

### Test Results

- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --stacktrace --rerun-tasks --no-build-cache` → **BUILD SUCCESSFUL**, 0 `FAILED`. Baseline preserved: 1168/1168 box + 1454/1456 phased (2 known won't-fix) + all `JavaParsingTest` unit tests.

### Key Learnings

- The `by lazy(PUBLICATION)` → `@Volatile` migration pattern from Step 3.6 applies beyond `JavaSyntaxNode`. Any model class instantiated in the thousands or more benefits from eliminating delegate wrappers, especially when the class has many lazy properties.
- Caching intermediate results (`rawTypeNameParts`) that multiple lazy properties depend on is a simple, high-value win — 4 independent `split('.')` calls on the same string is easy to miss in review because each lives in its own lazy block.
- For recursive resolution methods that split/join strings, refactoring to pass `List<String>` + `subList()` views eliminates O(n²) allocation without changing semantics — `subList` is a zero-copy view.

---

## Performance Optimizations Round 2: Lazy Delegate Elimination + Resolve Cache — 2026-04-17

### Overview

Continuation of performance optimization work from PERFORMANCE_REVIEW.md. This round focuses on:
- Eliminating `by lazy(PUBLICATION)` delegates from remaining model classes (items 1.1, 8 from the roadmap)
- Avoiding unnecessary HashMap allocation in `resolve()` (item 3.1)
- Reducing overlapping work between `classifier` and `classifierQualifiedName` (item 2.2)

### Optimization 6 — Skip HashMap allocation in `resolve()` for simple names

**Problem** (PERFORMANCE_REVIEW.md §3.1): Every call to `JavaResolutionContext.resolve()` created a fresh
`HashMap<ClassId, Boolean>` to cache `tryResolve` results within recursive prefix splitting. Most type
references are simple names (no dots) that don't benefit from this cache, but still paid the HashMap
allocation cost (~128 bytes per HashMap). For a file with 200 type references, this meant 200 short-lived
HashMaps with most of them unnecessary.

**Fix** (`JavaResolutionContext.kt`): Moved HashMap creation inside the `name.contains('.')` branch.
Simple names now call `resolveSimpleNameToClassId` directly with the raw `tryResolve` callback,
eliminating ~80% of HashMap allocations.

### Optimization 7 — Leverage cached `classifier` in `classifierQualifiedName`

**Problem** (PERFORMANCE_REVIEW.md §2.2): `classifierQualifiedName` independently called
`findTypeParameter(parts[0])` and `findLocalClass(Name.identifier(parts[0]))`, duplicating the same
lookups that `classifier` already performs and caches. Since both are `@Volatile`-cached, the second
property could simply read the first.

**Fix** (`JavaTypeOverAst.kt`): `computeClassifierQualifiedName()` now reads the cached `classifier`
property first. If it resolved to a `JavaTypeParameter`, returns `rawTypeName`. If it resolved to a
`JavaClass`, returns its FQN. Only falls through to import-based resolution when `classifier` is null
(external/cross-file classes).

### Optimization 8 — Replace 16 `by lazy(PUBLICATION)` delegates on `JavaClassOverAst`

**Problem** (PERFORMANCE_REVIEW.md §1.1): `JavaClassOverAst` had 16 lazy delegates (512 bytes per instance).
With ~5,000 class instances in a large project, this is ~2.5 MB of pure delegate overhead.

**Fix** (`JavaClassOverAst.kt`): Replaced all 16 delegates with `@Volatile`-backed manual caching:
- Reference types (`memberResolutionContext`, `typeParameters`, `supertypes`, `innerClassNames`,
  `methods`, `fields`, `constructors`, `recordComponents`, `annotations`): `@Volatile T? = null`
- Nullable results (`fqName`, `modifierList`): `@Volatile Any? = NOT_COMPUTED` sentinel
- Booleans (`isInterface`, `isAnnotationType`, `isEnum`, `isRecord`, `isSealed`): `@Volatile Int`
  tri-state (-1/0/1)

### Optimization 9 — Replace 15 `by lazy(PUBLICATION)` delegates on JavaMemberOverAst hierarchy

**Problem** (PERFORMANCE_REVIEW.md §1.1): `JavaMethodOverAst` (~50K instances, 5 delegates = 160 bytes),
`JavaFieldOverAst` (~30K instances, 5 delegates = 160 bytes), `JavaConstructorOverAst` (3 delegates),
and the shared `JavaMemberOverAst` base (2 delegates) together accounted for ~13 MB of delegate overhead.

**Fix** (`JavaMemberOverAst.kt`): Same `@Volatile` pattern applied across the hierarchy:
- `JavaMemberOverAst`: `modifierList` (sentinel), `annotations` (null)
- `JavaFieldOverAst`: `isEnumEntry` (tri-state), `leadingFieldNode` (sentinel), `effectiveModifierList`
  (sentinel), `annotations` (null), `type` (null)
- `JavaMethodOverAst`: `typeParameters`, `resolutionContext`, `valueParameters`, `returnType` (null),
  `modifierList` (sentinel)
- `JavaConstructorOverAst`: `typeParameters`, `resolutionContext`, `valueParameters` (null)

### Files Modified

| File | Change |
|------|--------|
| `JavaResolutionContext.kt` | Skip HashMap for simple names in `resolve()` |
| `JavaTypeOverAst.kt` | `classifierQualifiedName` leverages cached `classifier` |
| `JavaClassOverAst.kt` | 16 lazy delegates → `@Volatile` manual caching |
| `JavaMemberOverAst.kt` | 15 lazy delegates → `@Volatile` manual caching across 4 classes |

### Estimated Memory Impact

| Class | Instances | Old overhead | New overhead | Savings |
|-------|-----------|-------------|-------------|---------|
| `JavaClassOverAst` | 5K | 512 B/inst | ~128 B/inst | ~1.9 MB |
| `JavaMethodOverAst` | 50K | 160 B/inst | ~40 B/inst | ~6 MB |
| `JavaFieldOverAst` | 30K | 160 B/inst | ~48 B/inst | ~3.4 MB |
| `JavaConstructorOverAst` | 5K | 96 B/inst | ~24 B/inst | ~0.4 MB |
| `JavaMemberOverAst` (base) | 85K | 64 B/inst | ~16 B/inst | ~4 MB |
| **Total** | | | | **~15.7 MB** |

Combined with Round 1's ~36 MB savings on `JavaClassifierTypeOverAst`, total delegate overhead
reduction is ~52 MB for large projects.

### Test Results

All 4 optimizations verified independently: `./gradlew :kotlin-java-direct:test` → **BUILD SUCCESSFUL** after each change.

---

## Archives

| Archive | Iterations / Scope | Result |
|---------|--------------------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | Iters 1–6 | 0 → 90/138 box (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | Iters 7–16 | 90 → 1075/1166 box (92.2%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | Iters 17–23 | 1075 → 1150/1167 box, 1374/1442 phased (95.3%) |
| `implDocs/archive/ITERATIONS_24_26_DETAILS.md` | Iters 24–26 | 1150/1167 → same, phased 300 → 1374/1442 |
| `implDocs/archive/ITERATIONS_27_36_DETAILS.md` | Iters 27–36 | 1150/1167 → 1157/1168 box, **79 combined failing** |
| `implDocs/archive/ITERATIONS_37_51_DETAILS.md` | Iters 37–51 | 1157/1168 → 1165/1168 box, **17 combined failing** |
| `implDocs/archive/ITERATIONS_52_71_DETAILS.md` | Iters 52–71 | 1165/1168 → 1168/1168 box, 1454/1456 phased, **2 won't-fix**; perf + refactoring |
| `implDocs/archive/REFACTORING_STEPS_2026_04_17_DETAILS.md` | Refactoring Steps 1.3–3.6 (2026-04-17) | Baseline preserved; file split, VFS integration, memory footprint reduction, assorted perf wins |

---

## Future Iteration Template

~~~markdown
## Iteration N: [Title] - YYYY-MM-DD

### Root Cause Analysis
[Reference-first: check javac-wrapper / PSI / git show origin/master first]

### Fix
[Files modified, solution description]

### Test Results
- Box: X/1168, Phased: X/1443, Total failing: N

### Key Learnings
~~~
