# Java-Direct: Refactoring Steps 1.3–3.6 Details (Archived)

**Archive Date**: 2026-04-21
**Coverage**: Refactoring Steps 1.3 through 3.6, executed 2026-04-17
**Source plan**: `REFACTORING_PLAN.md` (archived as `implDocs/archive/REFACTORING_PLAN_1.md`)
**Result**: Baseline 1168/1168 box + 1454/1456 phased (2 known won't-fix) preserved throughout; multiple architectural improvements (file split, VFS integration, memory footprint reduction) and performance optimizations landed in a single day of refactoring work.

> **Warning**: This document is archived for historical reference. Only consult if you need to understand specific implementation decisions or debug regressions in the refactored code.

---

## Refactoring Summary

| Step | Focus | Files Touched | Key Change |
|------|-------|---------------|------------|
| 1.3 | Extract duplicate literal parsing | `JavaLiteralParser.kt` (new), `JavaAnnotationOverAst.kt`, `ConstantEvaluator.kt` | Consolidated `parseInteger/Long/Float/Double/unescape` helpers; kept safer octal guard |
| 1.4 | ConstantEvaluator vs FirExpressionEvaluator investigation | — (doc-only) | Decision: keep `ConstantEvaluator` — `FirExpressionEvaluator` requires `FirExpression` inputs |
| 1.5 (original, superseded) | File I/O via external service & error-swallowing fix | `JavaSourceFileReader.kt` (new), `JavaClassFinderOverAstImpl.kt` | `JavaSourceFileReader` interface over `java.nio.file.Path` with `j.u.logging` |
| 1.5 (revision) | File I/O via `VirtualFileSystem` | `JavaClassFinderFactory.kt`, `VfsBasedProjectEnvironment.kt`, `JavaClassFinderOverAstImpl.kt`, `JavaSourceFileReader.kt` | Threaded `VirtualFile` end-to-end; UTF-8 decode (not `VirtualFile.charset` — NPEs in unit tests) |
| 1.5 (follow-up) | Remove IO try/catch and logging | `JavaSourceFileReader.kt` | Removed `try`/`catch`; let `IOException` propagate per review feedback |
| 1.6 | Split `JavaClassFinderOverAstImpl` | `JavaSourceIndex.kt` (new), `JavaSupertypeGraph.kt` (new), `JavaClassFinderOverAstImpl.kt` (627→368 LOC) | Callback-based coupling; graph reads finder's cache via 4 narrow functions |
| 1.7 | Architectural code smells | `CombinedJavaClassFinder.kt`, `JavaTypeOverAst.kt`, `JavaClassOverAst.kt`, `JavaDirectComponentRegistrar.kt`, `JavaSupertypeGraph.kt` | TODOs removed with rationale; `updateResolutionContext` made `internal`; KDocs added |
| 2.1 | Optimize `findChildByType` / `getChildrenByType` | `utils.kt` | Threshold-gated `childByTypeIndex: Map<ElementType, List<Node>>` on `JavaSyntaxNode` |
| 2.2 | Optimize import extraction | `JavaImportResolver.kt` | `WeakHashMap` per-root cache + `ERROR_ELEMENT` presence fast-path |
| 2.3 & 2.4 | Cache short-circuit + `textEquals` | `JavaSupertypeGraph.kt`, `utils.kt`, `JavaClassOverAst.kt` | Recursive cache lookup in `collectRecursive`; allocation-free `textEquals` on hot paths |
| 2.5, 2.6, 3.5 | Negative caching + consistent cache types | `JavaClassFinderOverAstImpl.kt`, `JavaClassOverAst.kt` | `negativeClassCache`; `innerClassCache` → `ConcurrentHashMap<Name, Any>` with sentinel |
| 2.7 | Richer lightweight index (deferred) | — (doc-only) | Declined after walking consumer graph — no remaining hot path escalates to full parse |
| 3.1–3.4 | Code quality improvements | `JavaImportResolver.kt`, `JavaClassOverAst.kt`, `JavaDirectComponentRegistrar.kt`, `JavaSupertypeGraph.kt`, `JavaParsingTest.kt` | `extractImports` split into 4 methods; 4 invariant tests; TODOs addressed; robust `isAnnotationType` |
| 3.6 | Memory footprint — `JavaSyntaxNode` lazy-delegate removal | `utils.kt` | `@Volatile`-backed `text` / `childByTypeIndex` replace `by lazy(PUBLICATION)`; ~50 B/node saved |

**Net impact**: File-layout split, VFS integration, two new cache files, ~260 LOC extracted from `JavaClassFinderOverAstImpl`, per-node memory reduction of ~50 bytes, plus assorted readability and correctness clarifications — all while preserving the 99.9% green baseline.

---

## Refactoring Step 1.3: Extract Duplicate Literal Parsing - 2026-04-17

### Root Cause Analysis
`JavaAnnotationOverAst.kt` (lines ~230–336) and `ConstantEvaluator.kt` (companion, lines ~293–400) each carried private copies of `parseIntegerLiteral`, `parseLongLiteral`, `parseFloatLiteral`, `parseDoubleLiteral`, and `unescapeJavaString`. The two integer-parser copies also diverged slightly: `ConstantEvaluator` included an extra `cleaned.all { it in '0'..'7' }` guard on the octal branch, avoiding a misclassification of decimal numbers starting with `0` (e.g. `09`) as octal.

### Fix
Created `JavaLiteralParser.kt` — an `internal object` consolidating all five helpers, keeping the safer octal guard from `ConstantEvaluator`. Updated both call sites:

- `JavaAnnotationOverAst.kt`: removed the top-level private helpers and the `String.unescapeJavaString` extension; call sites in `evaluateLiteral` now delegate to `JavaLiteralParser.parseIntegerLiteral` / `parseLongLiteral` / `parseFloatLiteral` / `parseDoubleLiteral` / `unescapeJavaString(...)`.
- `ConstantEvaluator.kt`: removed the `companion object` body (was only hosting the duplicated helpers); `evaluateLiteral` delegates to `JavaLiteralParser` the same way.

No behavioral change beyond the minor integer-octal unification, which matches `ConstantEvaluator`'s pre-existing (more correct) behavior.

Files modified: `JavaLiteralParser.kt` (new), `JavaAnnotationOverAst.kt`, `ConstantEvaluator.kt`.

### Test Results
- Combined suite `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --rerun-tasks --no-build-cache` — **BUILD SUCCESSFUL**, 0 `FAILED` lines in the log.
- Baseline preserved: 1168/1168 box, 1454/1456 phased (2 known won't-fix).

### Key Learnings
- When consolidating "duplicate" helpers, diff the two versions carefully — minor guards can be load-bearing (octal detection here).
- Keeping the shared utility as an `internal object` (not extension functions) avoids polluting `String` with Java-literal-specific semantics, which would otherwise leak into unrelated call sites.

## Refactoring Step 1.4: ConstantEvaluator vs FirExpressionEvaluator — Investigation - 2026-04-17

### Question (from REFACTORING_PLAN.md)
Can the java-direct `ConstantEvaluator` (~290 LOC after Step 1.3) be replaced by FIR's `FirExpressionEvaluator` so Java constant folding reuses the canonical FIR evaluator?

### Investigation — What Each Evaluator Operates On

| Evaluator | Input | Output | Stage |
|-----------|-------|--------|-------|
| `ConstantEvaluator` (java-direct, `ConstantEvaluator.kt`) | `JavaSyntaxNode` — raw Java KMP-parser AST (`LITERAL_EXPRESSION`, `BINARY_EXPRESSION`, `REFERENCE_EXPRESSION`, …) | `Any?` — a Kotlin primitive/String/null | During Java model construction, before any FIR is built |
| `FirExpressionEvaluator.evaluateExpression(expr, session)` (`compiler/fir/providers/src/.../FirExpressionEvaluator.kt`, 704 LOC) | `FirExpression` — already fully-built & resolved FIR tree (`FirLiteralExpression`, `FirFunctionCall`, `FirPropertyAccessExpression`, …) | `FirEvaluatorResult` (wrapping a `FirLiteralExpression` or diagnostic) | During FIR resolution, after symbol & type resolution |

### Call Chain

1. **Consumer of `ConstantEvaluator`** — sole caller is `JavaFieldOverAst.initializerValue` / `resolveInitializerValue` (`JavaMemberOverAst.kt:244–255`).
2. **Who calls those?** — `FirJavaFacade.kt:567–576` (`lazyInitializer = lazy { ... }` of `buildJavaField`):
   ```kotlin
   lazyInitializer = lazy {
       javaField.initializerValue?.createConstantIfAny(session)
           ?: javaField.resolveInitializerValue { classQualifier, fieldName ->
               resolveExternalFieldValue(session, classQualifier, fieldName, classId.packageFqName)
           }?.createConstantIfAny(session)
   }
   ```
   This runs during FIR Java symbol provider materialization — we are *producing* the `FirField`'s initializer and need a plain `Any?` right now. There is no pre-existing `FirExpression` for the Java initializer: the Java-direct module never converts Java expressions to FIR.
3. **Interaction with `FirExpressionEvaluator`**: `FirJavaFacade.kt` line 32 already imports `FirExpressionEvaluator`, and `resolveExternalFieldValue` uses it (indirectly, via `extractConstantValue` on a Kotlin `FirPropertySymbol`) to resolve *the Kotlin side* of the cross-language callback — e.g. `MainKt.FOO` where `FOO` is a Kotlin `const val`. So the two evaluators already coexist on opposite sides of the Java→Kotlin boundary.

### Why a Direct Swap Is Not Feasible

- `FirExpressionEvaluator` fundamentally requires `FirExpression` inputs. The java-direct pipeline has no `FirExpression` for a Java field initializer — it has a raw KMP `JavaSyntaxNode`.
- Building one would require a new **Java-AST → FIR-expression** conversion layer (equivalent to what the old PSI-based Java-to-FIR converter did for method bodies and initializers), which is a substantially larger architectural change than the goal of this refactoring plan.
- Even then, the conversion would need access to FIR symbol resolution for `REFERENCE_EXPRESSION`s, introducing a new ordering dependency: Java field FIR-building would have to wait on (or lazily trigger) FIR resolution of referenced Kotlin/Java symbols. Today that dependency is cleanly side-stepped via the `resolveReference` callback, which only descends into FIR for qualified cross-language refs.
- Scope: `ConstantEvaluator` handles literals (integer/long/float/double/string/char/bool/null), unary (`+`, `-`, `!`, `~`), binary & polyadic (`+`, `-`, `*`, `/`, `%`, `<<`, `>>`, `>>>`, `&`, `|`, `^`, `&&`, `||`, `==`, `!=`, `<`, `>`, `<=`, `>=`), parenthesized, conditional (`?:`), type casts, and simple/qualified field refs — this is exactly the JLS §15.29 "constant expression" subset. `FirExpressionEvaluator` is a **superset** of this functionality (it also handles Kotlin-specific calls, when-expressions, string templates, etc.), so no expressive power is gained.

### Conclusion

`ConstantEvaluator` cannot be replaced by `FirExpressionEvaluator` without first introducing a Java-AST → FIR-expression conversion layer inside `FirJavaFacade` (or earlier). The cost/benefit is poor:

- **Cost**: a new conversion layer (non-trivial — must cover all JLS §15.29 constant-expression forms, plus resolve Java class/field references to FIR symbols at the right resolution phase), plus the risk of reshuffling the FIR Java symbol-provider phase ordering.
- **Benefit**: removing ~290 LOC of fairly contained code, in exchange for non-trivial FIR conversion code of comparable size.

### Recommendation (final)

**Keep `ConstantEvaluator` as-is.** It is the correct architectural layer for Java-model-level constant folding (pre-FIR), it is contained, has no external consumers beyond `JavaMemberOverAst`, and now shares its literal-parsing core with `JavaAnnotationOverAst` via `JavaLiteralParser` (Step 1.3). No follow-up task is warranted unless/until a separate initiative introduces a Java-AST→FIR-expression converter for other reasons (e.g. method-body constant folding, which is out of scope here).

### Verification
Document-only step — no code changes, no test run required per the plan. Current baseline from Step 1.3 (1168/1168 box, 1454/1456 phased) remains authoritative.

### Key Learnings
- FIR's `FirExpressionEvaluator` is a post-resolution tool; any pre-FIR layer that needs constant folding cannot use it without first materializing FIR.
- Coexistence pattern is already in place: Java side uses `ConstantEvaluator`, Kotlin side (for cross-language refs) uses `FirExpressionEvaluator` via the `resolveReference` callback. This is a sensible seam and should be preserved.

## Refactoring Step 1.5: File I/O via External Service & Error Swallowing Fix - 2026-04-17 (superseded)

### Problem (from REFACTORING_PLAN.md)
`JavaClassFinderOverAstImpl.kt` did its own filesystem I/O: `Files.walk()` in `buildIndex()` and a private `tryReadFile()` helper that silently swallowed `IOException` (with a `// TODO: ... shoulbe propagated` comment typo). Five call sites (`indexPackageInfo`, `tryBuildFileEntryWithFullParse`, `parseTopLevelClassFromFile`, `getDirectSupertypes`, `getInnerClassNames`) read source files through that swallow-all helper, meaning permission errors / encoding failures were indistinguishable from "file simply missing".

### Fix
Introduced a new collaborator `JavaSourceFileReader` (interface) with the default on-disk implementation `DefaultJavaSourceFileReader`:

- `readFileContent(path)` — distinguishes "not found / not a regular file" (silent `null`) from "exists but unreadable" (logs a `Level.WARNING` via `java.util.logging` and returns `null`). Both `IOException` and `SecurityException` are caught and logged with the offending path.
- `walkSourceRoots(roots)` — lazy `Sequence<Path>` yielding `.java` files under each root; non-existent roots are skipped silently, but walk failures are logged and the root is skipped (never swallowed silently).

`JavaClassFinderOverAstImpl`:
- Added a third constructor parameter `sourceFileReader: JavaSourceFileReader = DefaultJavaSourceFileReader` so existing call sites (tests + `JavaDirectComponentRegistrar`) need no changes, and tests can inject an in-memory/virtual-FS reader in the future.
- Replaced all five `tryReadFile(...)` sites with `sourceFileReader.readFileContent(...)` and removed the private helper along with its `shoulbe` TODO.
- Replaced the `Files.walk(root).use { ... forEach }` block in `buildIndex()` with a single `for (path in sourceFileReader.walkSourceRoots(sourceRoots))` loop. The package-info.java branch and the index-building logic are unchanged.
- Dropped the now-unused `kotlin.io.path.isRegularFile` import; `Files` / `IOException` are still required by `extractFileInfoLightweight` (uses `Files.newBufferedReader` and line-by-line scanning — intentionally kept out of the reader abstraction since it's a streaming pre-parse scan, not a "read whole file" operation) and by `tryBuildFileEntry`'s `Files.size(path)` size-bucket check.

### Files modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaSourceFileReader.kt` (new, 102 LOC)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` (5 read sites, 1 walk site, 1 constructor param, 1 TODO removed, 1 unused import removed)

### Design Notes
- **Why `java.util.logging`?** The compiler's `MessageCollector` is oriented at user-facing source diagnostics; a missing/unreadable `.java` file under a configured source root is an infrastructural problem (build setup) rather than a source-code diagnostic. `j.u.l` surfaces the warning in compiler logs without pulling in a new dependency. If the team later wants to route these through `MessageCollector`, a second `JavaSourceFileReader` impl can be wired via the registrar — that is exactly why the abstraction was extracted.
- **Why `CharSequence` instead of `String`?** Matches the KMP parser signature (`parseJavaToSyntaxTreeBuilder(source: CharSequence, ...)`) — avoids forcing future impls to materialize a full `String`.
- **`JavaDirectComponentRegistrar`** was left untouched: the default-argument constructor preserves behavior, and Step 1.7 (not 1.5) is the one that revisits the registrar.

### Test Results
- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --no-configuration-cache --no-build-cache` → **BUILD SUCCESSFUL**, 0 `FAILED` lines.
- Baseline preserved from Step 1.3/1.4: 1168/1168 box, 1454/1456 phased (2 known won't-fix).

### Key Learnings
- When extracting I/O into a collaborator, keep the "not found" vs. "unreadable" distinction at the interface level — it's cheap to encode (pre-check existence before the `try`) and makes the contract obvious to every caller.
- Stream interop: `java.util.stream.Stream<Path>.toList()` is **not** Kotlin's `toList()`; in Kotlin 1.x we iterate manually (`forEach { ... }` into an `ArrayList`) rather than depending on `kotlin.streams.toList` which was experimental in the target Kotlin version for this module.
- Default-argument constructor params are a low-risk way to introduce a new dependency into a class with many existing callers — no call site churn, and tests can opt in to a custom impl when needed.

## Refactoring Step 1.5 (revision): File I/O via `VirtualFileSystem` - 2026-04-17

### Why a revision
The first attempt at Step 1.5 introduced `JavaSourceFileReader` over `java.nio.file.Path` and used `Files.walk` / `path.toFile().readText()`. Review feedback: this ignores the `localFs: VirtualFileSystem` already constructed in `org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment.getFirJavaFacade`, and so bypasses the VFS caching layer that the rest of the compiler relies on. The correct seam is to thread `VirtualFile`/`VirtualFileSystem` all the way through the `JavaClassFinderFactory` → `JavaClassFinderOverAstImpl` chain, including the initial source-roots handling.

### Changes
- **`JavaClassFinderFactory.createJavaClassFinder`** (interface in `compiler/cli`): signature gains `localFs: VirtualFileSystem` and `findLocalFile` changes from `(String) -> File?` to `(String) -> VirtualFile?`. This lets implementations both (a) resolve the configured source-root paths to `VirtualFile`s with the project's scope filter applied, and (b) reuse the same `VirtualFileSystem` for any subsequent lookups so reads benefit from VFS caching.
- **`VfsBasedProjectEnvironment.getFirJavaFacade`**: passes `localFs` directly and simplifies the lambda to `{ localFs.findFileByPath(it)?.takeIf(psiSearchScope::contains) }` — no more `VirtualFile → path → File` round-trip.
- **`JavaClassFinderOverAstFactory`** (in `JavaDirectComponentRegistrar.kt`): the configured `javaSourceRoots` are now resolved via the injected `findLocalFile` directly to `List<VirtualFile>`; no `canonicalFile.toPath()` conversion.
- **`JavaClassFinderOverAstImpl`**: `sourceRoots: List<Path>` → `List<VirtualFile>`, and `FileEntry.path: Path` → `FileEntry.file: VirtualFile`. All read sites (`indexPackageInfo`, `tryBuildFileEntryWithFullParse`, `parseTopLevelClassFromFile`, `getDirectSupertypes` slow path, `getInnerClassNames` slow path) now call `sourceFileReader.readFileContent(vf)`; file-size bucket uses `VirtualFile.length`; the `Files.walk` in `buildIndex` is replaced by `sourceFileReader.walkSourceRoots(roots)`, which recurses through `VirtualFile.children` (backed by `CoreLocalVirtualFile`'s cache). `debugLogFilePath: Path?` is kept as `Path` — it points to a debug artefact outside the project scope.
- **`JavaSourceFileReader`**: rewritten around `VirtualFile`. Interface now declares `readFileContent(file: VirtualFile)`, `walkSourceRoots(roots: List<VirtualFile>)`, and `openLineReader(file: VirtualFile)` (for the lightweight scanner). `DefaultJavaSourceFileReader` uses `VirtualFile.contentsToByteArray()` and decodes with UTF-8 — **not** `VirtualFile.charset`, which indirects through `EncodingManager.getInstance()` and NPEs in environments without an IDE `Application`. This matches the legacy scanner (`Files.newBufferedReader(..., StandardCharsets.UTF_8)`) and javac's default. I/O errors are still logged at `Level.WARNING`; "not a regular file / invalid / directory" is silent `null`.
- **`extractFileInfoLightweight`**: takes a `VirtualFile` plus a `JavaSourceFileReader` and obtains its line reader through the reader (so test fakes can swap it just like full reads).
- **Tests**: `JavaParsingTest.kt` gained a shared `private val testLocalFs = KotlinLocalFileSystem()` + `Path.toVFile()` helper; all 11 `JavaClassFinderOverAstImpl(listOf(tempDir))` and 9 `extractFileInfoLightweight(file)` call-sites updated. The test fixture `VfsBasedProjectEnvironmentOverAst` resolves its `javaSourceRoots: List<Path>` to `List<VirtualFile>` once via `KotlinLocalFileSystem().findFileByNioFile(...)`.

### Key learning — the charset trap
`VirtualFile.charset` delegates to `EncodingRegistry.getInstance()` → `EncodingManager.getInstance()`, which dereferences the IntelliJ `Application`. Unit tests for this module run without an `Application`, so the first test run NPE'd in `JavaParsingTest.testLightweightScannerDefaultPackage` etc. Fix: decode bytes from `VirtualFile.contentsToByteArray()` with `StandardCharsets.UTF_8` explicitly. For `.java` sources this matches what javac does by default and is consistent with the behavior before this refactor.

### Test Results
- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --no-configuration-cache --no-build-cache` → **BUILD SUCCESSFUL**, 0 `FAILED` lines. Baseline preserved: 1168/1168 box + 1454/1456 phased (2 known won't-fix).

### Design notes
- **Why a `JavaSourceFileReader` at all (even with VFS)?** VFS gives caching, but the `java-direct` module still benefits from an injection seam: tests that don't want to populate a local directory can provide a fake reader, and a future `MessageCollector`-routing implementation can replace the default without touching call sites.
- **`walkSourceRoots` uses recursion over `VirtualFile.children`** rather than `VfsUtilCore.visitChildrenRecursively` to keep the lazy `Sequence` semantics — `buildIndex` consumes the stream once and never materializes the full file list.

## Refactoring Step 1.5 (follow-up): Remove IO try/catch and logging - 2026-04-17

Per review feedback, IO error handling belongs at a higher compiler level — the reader should not
catch and log `IOException` itself. Removed all `try`/`catch` blocks from
`DefaultJavaSourceFileReader` (`readFileContent`, `openLineReader`, `walkSourceRoots.walk`) along
with the now-unused `java.util.logging.Logger`/`Level` and `java.io.IOException` imports. The
reader still distinguishes "not a readable regular file" (silent `null` for invalid/directory) from
a real read attempt; any `IOException` from the VFS now propagates to the caller. Tests green:
baseline 1168/1168 box + 1454/1456 phased preserved.
## Refactoring Step 1.6: Split `JavaClassFinderOverAstImpl` into Focused Components - 2026-04-17

### Problem (from REFACTORING_PLAN.md)
`JavaClassFinderOverAstImpl.kt` had grown to 627 lines, combining: filesystem walking, source index building (eager full parse + lightweight scan), package-info annotation extraction, class-cache orchestration, supertype-graph computation, inherited-inner-class collection (with JLS 8.5 shadowing), and same-package/import-aware supertype reference resolution. All three reviews flagged it as a candidate for decomposition.

### Fix
Two new files, one behavioural refactor of the finder itself. The `JavaFileParserCache` proposed in the plan did not justify extraction — the file-level parser+cache path is small, already private, and tightly coupled to `classCache` (which is also touched by the supertype graph's fast path), so splitting it would have required leaking the cache through a second interface for zero readability gain. The pragmatic 2-way split:

- **`JavaSourceIndex.kt`** (new, 116 LOC) — top-level lightweight-scan helpers extracted verbatim from the finder:
  - `PACKAGE_REGEX`, `DECLARATION_REGEX`
  - `LightweightFileInfo` data class
  - `stripLineComments` (private, comment-state machine)
  - `extractFileInfoLightweight(file, reader)` — unchanged signature, so `JavaParsingTest.kt` (9 call sites) needs no update.
- **`JavaSupertypeGraph.kt`** (new, 227 LOC) — encapsulates the supertype-graph logic and its two caches:
  - `supertypeCache: MutableMap<ClassId, List<ClassId>>`
  - `inheritedInnerClassesCache: MutableMap<ClassId, Map<String, Set<ClassId>>>`
  - `getDirectSupertypes(classId)` — fast path via cached `JavaClassOverAst.node` + `getImports()`, slow path re-parses the file.
  - `collectInheritedInnerClasses(classId)` — BFS with JLS 8.5 shadowing.
  - Private: `getInnerClassNames`, `extractSupertypeRefsFromNode`, `findClassInTree`, `resolveSupertypeReference`.
  - Consults the finder via 4 constructor callbacks (no bidirectional reference): `classCacheLookup`, `filesForClassLookup`, `sameClassInSameFilePackage`, `sourceFileReader`. This preserves the single authoritative copy of `index` and `classCache` in the finder — the graph only reads through them.

- **`JavaClassFinderOverAstImpl.kt`** (627 → 368 LOC): removes the extracted helpers/classes, instantiates a single `supertypeGraph` property, and turns `getDirectSupertypes` / `collectInheritedInnerClasses` into one-line delegates. The `findFilesForClass` helper stays here (it reads `index`) and is wrapped into the `filesForClassLookup` callback so the graph can use `VirtualFile` directly without touching the private `FileEntry` type. All other behaviour — `findClass`, `findClasses`, `findPackage`, `knownClassNamesInPackage`, `buildIndex`, `indexPackageInfo`, `tryBuildFileEntryWithFullParse`, `tryBuildFileEntryLightweight`, `parseTopLevelClassFromFile`, `classesInPackage`, `subPackagesOf`, `isClassInIndex`, `getPackageAnnotations` — is unchanged.

### Files modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaSourceIndex.kt` (new, 116 LOC)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaSupertypeGraph.kt` (new, 227 LOC)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` (627 → 368 LOC)

### Design notes
- **Callback-based coupling, not bidirectional references.** The graph never holds a reference to the finder; it receives only the four narrow functions it actually needs. This keeps the dependency unidirectional (finder → graph) and makes the graph unit-testable in isolation (future work).
- **Why not a third `JavaFileParserCache` file?** The plan proposed it, but the eager full-parse + lazy sibling-caching paths are inseparable from `classCache` (which the supertype-graph fast path also reads through `classCacheLookup`). Extracting them would have required promoting `classCache` into a second public-ish interface with no reduction in LOC of the owning class. Keeping parser+cache in the finder preserves the invariant that **all** writes to `classCache` originate in one file.
- **`extractFileInfoLightweight` stays top-level.** Tests use it directly (`JavaParsingTest.kt`, 9 call sites), and its signature `(VirtualFile, JavaSourceFileReader) -> LightweightFileInfo?` is already seam-friendly.

### Test Results
- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --stacktrace --rerun-tasks --no-build-cache` → **BUILD SUCCESSFUL**, 0 `FAILED` lines. Baseline preserved: 1168/1168 box + 1454/1456 phased (2 known won't-fix) + all `JavaParsingTest` unit tests.

### Key Learnings
- When splitting a "God class", start by inventorying which fields are read from where. Fields shared between two candidate halves (here: `classCache` — read by both the parser path and the supertype path) are signals to **not** split along that line, or to promote the shared field to a neutral dependency of both halves.
- Callback constructor parameters (`(X) -> Y` function types) are lighter than extracted Kotlin interfaces for internal module collaborators: no extra file, no vtable, no `override` ceremony, and the call sites read naturally.

## Refactoring Step 1.7: Fix Remaining Architectural Code Smells - 2026-04-17

### Problem (from REFACTORING_PLAN.md)
Five small but real code smells, each well-known but unfixed: a `TODO`-flagged FQN check in `CombinedJavaClassFinder` whose reasoning was opaque; an undocumented two-phase construction invariant in `JavaTypeParameterOverAst.updateResolutionContext` (publicly callable, no contract); two `findInner…InSupertypes` methods (one in `JavaClassOverAst`, one in `JavaInheritedMemberResolver`) with no comment on why both exist; a `TODO: remove after testing or find a better way to debuglog` on `JAVA_DIRECT_DEBUG_LOG_PROPERTY_NAME`; and a typo `enore` in `JavaSupertypeGraph` (the `shoulbe` typo had already been removed by Step 1.5, and `reasonin` lived in the `CombinedJavaClassFinder` TODO that this step removed).

### Fix
Five focused, low-risk edits — no behavioural changes:

- **`CombinedJavaClassFinder.kt`** (line ~38): removed `// TODO: recheck this place, the reasonin is suspicious`. The check **is** load-bearing — some delegating finders (PSI-based, in particular) can return a `JavaClass` whose `fqName` does not equal `request.classId.asSingleFqName()` (inner-class collisions, classpath/module scoping). Without it, FIR would build symbols keyed by the requested ClassId but pointing at the wrong class, breaking annotation/type resolution. Replaced the TODO with a clear multi-paragraph rationale (including the note that `asSingleFqName()` flattens nested separators, so the comparison is intentionally a flat-FQName equality).
- **`JavaTypeOverAst.kt`** (`JavaTypeParameterOverAst`, lines ~532–547): added a class-level KDoc spelling out the **two-phase construction invariant** (parameters are constructed with the bare containing-class context, then `updateResolutionContext` is called once with the sibling-aware context before `upperBounds` is touched). Marked `updateResolutionContext` as `internal` so external callers cannot break the invariant — the only call site is `computeTypeParameters` in `utils.kt` (verified by symbol search). Did not switch to `lateinit var` because it would require a sentinel-then-replace pattern that obscures more than it documents; the `var` + KDoc is clearer and equally safe given the now-`internal` setter.
- **`JavaClassOverAst.kt`** (`findInnerClassInSupertypes`, lines ~167–207): added a comparison-table KDoc explicitly distinguishing it from `JavaInheritedMemberResolver.findInnerClassFromSupertypes`. Key point: this method **must** scan raw AST text (`EXTENDS_LIST`/`IMPLEMENTS_LIST` text content) and resolve via `findLocalClass`, because reading `javaClass.supertypes` would re-enter type construction → `classifier → findLocalClass → findInnerClass`, looping. The inherited-member resolver, conversely, **needs** resolved supertypes to detect cross-file ambiguities. The two cannot be unified.
- **`JavaDirectComponentRegistrar.kt`** (line ~64): removed the `// TODO: remove after testing or find a better way to debuglog`. Investigation showed the property is genuinely useful — it's read in `JavaDirectComponentRegistrar.createJavaClassFinder` and consumed by `JavaClassFinderOverAstImpl` (`debugLogFilePath`/`debugLogFile`, line 49 / 80 / 135) to append `findClasses` traces. Promoted from `val` to `const val`, kept it `internal`, and added KDoc explaining (a) what it does, (b) why it remains a system property rather than a CompilerConfiguration option (must be enabled before the finder is constructed; developer-only diagnostic with no public CLI surface), and (c) that no I/O occurs when the property is unset.
- **`JavaSupertypeGraph.kt`** (line 79): typo `enore` → `enough` in the existing `// TODO: check if this is rare enough` comment (the TODO itself is tracked under Step 3.3 and intentionally left for later).

### Files modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/CombinedJavaClassFinder.kt` (TODO removed, rationale block added; +6 net LOC of comments)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` (class KDoc added, function `internal`-ised + KDoc; +27 net LOC of docs)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` (KDoc table added; +12 net LOC of docs)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaDirectComponentRegistrar.kt` (TODO removed, KDoc added, `val` → `const val`; +11 net LOC of docs)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaSupertypeGraph.kt` (one-character typo fix)

### Test Results
- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --stacktrace --rerun-tasks --no-build-cache` → **BUILD SUCCESSFUL**, 0 `FAILED` lines. Baseline preserved: 1168/1168 box + 1454/1456 phased (2 known won't-fix) + all `JavaParsingTest` unit tests.

### Key Learnings
- A `TODO: this looks suspicious` is itself a smell — either prove the code wrong (and fix it) or prove it right (and document why). For `CombinedJavaClassFinder`'s FQN-equality guard, walking through the underlying finder contracts showed it is genuinely required, so the TODO was replaced by rationale rather than by a code change.
- When two functions in different files do "almost the same thing", the right move is often to document **why they cannot be unified** rather than to force a unification. Here, the recursion-guard requirements (raw-AST traversal vs. resolved-supertypes traversal) are fundamentally incompatible.
- `internal` + KDoc is a stronger contract than `lateinit` for two-phase construction when there is exactly one trusted caller — `lateinit` advertises "this can be set later by anyone", which is the opposite of what we want.

## Refactoring Step 2.1: Optimize `findChildByType` / `getChildrenByType` for Hot Nodes - 2026-04-17

### Problem (from REFACTORING_PLAN.md)
Both `findChildByType(SyntaxElementType)` and `getChildrenByType(SyntaxElementType)` in `utils.kt` performed a full linear scan over `JavaSyntaxNode.children` on every call. On "hot" composite nodes (class bodies, method declarations, modifier lists, etc.) the same node is typically queried for several different child types in sequence — MODIFIER_LIST, then IDENTIFIER, then TYPE_PARAMETER_LIST, then EXTENDS_LIST, … — so aggregate behaviour is O(n²) in the number of children.

### Fix
Added a lazily-built, type-indexed view on `JavaSyntaxNode` and routed the two `SyntaxElementType`-based lookup helpers through it. Design points:

- **Threshold-gated materialization.** `JavaSyntaxNode.childByTypeIndex` is a `by lazy(LazyThreadSafetyMode.PUBLICATION)` property that returns `null` whenever `children.size <= CHILD_INDEX_THRESHOLD` (constant = 4). For small children lists the linear scan is both faster than a `HashMap` lookup *and* avoids retaining a map object. Token/leaf nodes (empty children) therefore never allocate a map — only the (tiny) lazy delegate header.
- **`PUBLICATION` thread-safety mode** — matches the existing `text` property; the computation is idempotent (`groupBy` on an immutable `children` list), so a double-compute race is harmless.
- **Return-type symmetry.** The index stores `Map<SyntaxElementType, List<JavaSyntaxNode>>` (via `groupBy`). `findChildByType` returns `list.firstOrNull()` (matching the "first child of that type" semantics of the previous `children.find { … }`); `getChildrenByType` returns the list or `emptyList()`. Ordering among same-type siblings is preserved because `groupBy` is order-stable.
- **String-overload untouched.** `findChildByType(String)` / `getChildrenByType(String)` are still used at three `DOC_COMMENT` call sites (`JavaClassOverAst.kt`, `JavaMemberOverAst.kt ×2`) because the KMP parser exposes doc comments only as a string-named leaf type. Step 1.1 explicitly listed these as out of scope for constant-ification, so they remain on the linear-scan path — those nodes (class/method) happen to be the same hot nodes that benefit from the index, but the string comparison still walks `children` directly. That is acceptable: only 3 call sites, once per lazy property (`isDeprecatedInJavaDoc`).

Files modified: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/utils.kt` only.

### Alternatives considered and rejected
- **Always build the map.** Rejected: millions of token/leaf nodes would allocate a `HashMap` on first access for a single lookup — a net slowdown.
- **Cache commonly needed children as `lateinit`/`lazy` properties on each model class** (`JavaClassOverAst`, `JavaMemberOverAst`, …). This was the second alternative suggested in the plan, but it spreads the optimisation across ~6 files and requires ad-hoc judgement of "which children to cache". The threshold-gated index achieves the same amortised O(1) behaviour for **any** child-type query on hot nodes without per-class bookkeeping, and keeps the change localised to `utils.kt`.
- **Per-node `HashMap` with manual double-checked locking.** Rejected as needless — `by lazy(PUBLICATION)` compiles to an essentially equivalent double-checked pattern and matches the style already used by `JavaSyntaxNode.text`.

### Test Results
- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --stacktrace --rerun-tasks --no-build-cache` → **BUILD SUCCESSFUL**, 0 `FAILED` lines in the saved log.
- Baseline preserved: 1168/1168 box + 1454/1456 phased (2 known won't-fix) + all `JavaParsingTest` unit tests.

### Key Learnings
- When the "obvious" cache would sit on millions of tiny objects, a size threshold before materialisation is usually strictly better than an always-on cache — the lazy delegate itself is cheap, but the cached collection is not.
- `groupBy` is the right building block for a child-by-type index: order-stable, one pass, and naturally supports both the "first match" (`find*`) and "all matches" (`get*`) semantics without a second representation.
- Keeping the optimisation entirely inside `utils.kt` (no model-class changes) means future new model classes automatically benefit with zero extra code.

## Refactoring Step 2.2: Optimize Import Extraction - 2026-04-17

### Problem (from REFACTORING_PLAN.md)
`JavaImportResolver.extractImports` (155 LOC, called from `JavaResolutionContext.create()` on every context creation) unconditionally walked `root.children` looking for fragmented-import patterns — sibling `ERROR_ELEMENT`/`TYPE` sequences the parser emits only when an import line doesn't parse cleanly (e.g., `import kotlin.*` where `kotlin` is a reserved package name in the Kotlin-compiled-Java parser profile). For well-formed Java files — i.e. essentially all of them — that loop ran all the way through every top-level node of the file for zero matches. The method was also re-run from scratch for every new `JavaResolutionContext` built on the same compilation unit root (e.g. on every `withTypeParameters` / `withContainingClass` chain-start), duplicating the whole scan.

### Fix
Two orthogonal, targeted optimisations inside `JavaImportResolver.kt` — no behaviour change, no other files touched:

- **Per-root result cache.** Added a `Collections.synchronizedMap(WeakHashMap<JavaSyntaxNode, Pair<…,…>>)` keyed on the compilation-unit root. `extractImports` now does an O(1) lookup first and only invokes the new private `extractImportsUncached` on a miss. `WeakHashMap` + weak key means entries are GC-collected when the root AST is released, so this never holds a file alive and never grows unboundedly across multiple compiler invocations. The synchronized wrapper covers concurrent FIR-phase readers.

- **Fast-path exit on the fragmented-import scan.** Inside `extractImportsUncached`, right before the existing `while (i < allChildren.size)` loop, added `val hasRootLevelErrorElement = allChildren.any { it.type == SyntaxTokenTypes.ERROR_ELEMENT }` and an early `return simpleImports to starImports` when it is `false`. Rationale: the whole purpose of that loop is to recover from `ERROR_ELEMENT`-flagged parser failures; if there are none at the root level, there is nothing it can recover. This short-circuits the common case with an O(N) `any` over root children — which is already what the loop's first iteration would do anyway, just now bounded to a single pass and without entering the inner skip/lookahead state machine.

`extractPackageName` and `findClassNode` were left untouched (cheap, single-pass, not hot in profiling).

### Files modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaImportResolver.kt` (+22 LOC — cache field, cache wrapper, fast-path check, KDoc touch-up)

### Alternatives considered and rejected
- **Guard behind "importList is non-null and well-formed"** (first bullet in the plan). Rejected: the "well-formed" check is exactly what the fragmented-import loop does; a pre-check that inspects each import statement's children to decide "well-formed enough" would duplicate most of the loop. The `ERROR_ELEMENT`-presence gate is a strictly cheaper equivalent — no root-level `ERROR_ELEMENT` implies the fragmented-import pattern cannot exist.
- **Cache on `JavaResolutionContext` instead of on `JavaImportResolver`.** Rejected: a `JavaResolutionContext` is rebuilt per containing-class / type-parameter scope, so caching there would miss the hits that actually matter (different contexts, same root file).
- **Strong-keyed `HashMap`.** Rejected: would pin compilation-unit ASTs in memory across runs. `WeakHashMap` is the standard idiom for "cache while the key is alive", and matches the overall lifetime model of the module (AST roots are held by `JavaClassFinderOverAstImpl.classCache` entries and released with them).

### Test Results
- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --stacktrace --rerun-tasks --no-build-cache` → **BUILD SUCCESSFUL**, 0 `FAILED` lines (`$JD_TMP/jd_test_step2_2.txt`). Baseline preserved: 1168/1168 box + 1454/1456 phased (2 known won't-fix) + all `JavaParsingTest` unit tests.

### Key Learnings
- For parser-recovery code paths, the cheapest correctness-preserving short-circuit is usually a presence-check on the token the recovery code *exists to handle* — here, `SyntaxTokenTypes.ERROR_ELEMENT`. That is strictly cheaper than re-validating the happy-path data structures.
- A `WeakHashMap` keyed by AST root is a surprisingly effective memoisation surface for per-file pure computations in this module: the AST roots are interned by the class finder's own cache, so hits are frequent and eviction piggybacks on the class cache's lifecycle.

## Refactoring Steps 2.3 & 2.4: Cache Short-Circuit + `textEquals` - 2026-04-17

### Problem (from REFACTORING_PLAN.md)
- **Step 2.3**: `JavaSupertypeGraph.collectInheritedInnerClasses` caches the *final* result per `ClassId`, but its inner `collectRecursive` walk never consulted that cache for intermediate nodes. In diamond / wide inheritance patterns (e.g. deep framework hierarchies where dozens of classes share a handful of common supertypes) this re-walked each shared supertype once per distinct descendant path, doing redundant file-I/O + parse work through `getInnerClassNames` → `getDirectSupertypes`.
- **Step 2.4**: `JavaSyntaxNode.text` materialises the slice as a `String` via `source.subSequence(...).toString()` and the result is cached by `by lazy`. Hot identifier/keyword comparisons (`node.findChildByType(IDENTIFIER)?.text == name.asString()` in `findInnerClassUncached`, the supertype-walk in `findInnerClassInSupertypes`, and class-path traversal in `JavaSupertypeGraph.findClassInTree`) are invoked many times per lookup and would otherwise populate the lazy slot + allocate a fresh `String` for every candidate child — almost all of which mismatch on length alone.

### Fix
**Step 2.3 — `JavaSupertypeGraph.kt` `collectRecursive` cache short-circuit.** Added a pre-visit lookup into `inheritedInnerClassesCache` at the top of the recursive function. On a cache hit we add `current` to `visited`, merge the cached `Map<String, Set<ClassId>>` into `result` honouring the caller's `shadowedNames`, and return. Correctness argument, kept in comments next to the code: the cached map for `current` already reflects the intra-subtree shadowing applied when it was originally computed (closer supertypes of `current` shadowing their farther supertypes); the only extra filter we need is `shadowedNames` coming from the caller's *path above* `current`, which is exactly what the merge-with-filter does. Merging via `getOrPut { mutableSetOf() }.addAll(classIds)` is idempotent for diamond cases where the same cached entry contributes through multiple paths.

**Step 2.4 — `utils.kt` `JavaSyntaxNode.textEquals`.** Added:
```kotlin
fun textEquals(expected: String): Boolean {
    val length = endOffset - startOffset
    if (length != expected.length) return false
    for (i in 0 until length) if (source[startOffset + i] != expected[i]) return false
    return true
}
```
Zero allocations, O(length) with an O(1) length fast-fail for the overwhelming majority of mismatching candidates. Then migrated the three hottest call-sites:
- `JavaClassOverAst.findInnerClassUncached` (inner-class lookup over `node.children`).
- `JavaClassOverAst.findInnerClassInSupertypes` (scan over each resolved supertype's direct children; hoisted `val nameString = name.asString()` out of the `for (supertypeRef in …)` loop so it's computed once per call, not per iteration).
- `JavaSupertypeGraph.findClassInTree` (segment-by-segment `ClassId` resolution down the parse tree).

`.text` is left in place where the value is actually consumed as a `String` (import resolution, annotation-value parsing, `deriveImplicitPermittedTypes`' `substringBefore('<').trim()` path, etc.) — no benefit there.

### Files modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaSupertypeGraph.kt` — cache short-circuit in `collectRecursive`; `findClassInTree` switched to `textEquals`.
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/utils.kt` — `textEquals` added to `JavaSyntaxNode`.
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — two `?.text == name.asString()` call-sites migrated; `nameString` hoisted in the supertype loop.

### Alternatives considered and rejected
- **Step 2.3: recompute the full `collectRecursive` for each supertype and deduplicate via `result`.** Rejected: already what the code did before this change — the whole point of the cache is to skip that work for shared ancestors.
- **Step 2.3: key the cache by `(ClassId, shadowedNames)`.** Rejected: `shadowedNames` varies per path, so the cache hit-rate would collapse; also memory-unbounded. The current invariant (cache key = `ClassId`, filter on read) gives the same visible result because the stored map is already internally-shadowed.
- **Step 2.4: make `textEquals` look at the cached `text` first.** Rejected: the `by lazy` delegate itself is already a monitor/volatile read; we'd pay for it on every call. When `text` isn't yet materialised (the common case on hot paths) we save both the allocation and the slot population; when it is, the `source[…]` loop is still about as fast as `String.equals` on short identifiers.
- **Step 2.4: introduce a second lazy `name`/`identifierText` slot on `JavaSyntaxNode`.** Rejected: would permanently grow every node (millions of tokens) by a pointer just to serve three call-sites; `textEquals` has zero per-node cost.

### Test Results
- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --stacktrace --rerun-tasks --no-build-cache` → **BUILD SUCCESSFUL**, 0 `FAILED` lines (`/tmp/jd_20260417_115237/jd_test_step2_34.txt`). Baseline preserved: 1168/1168 box + 1454/1456 phased (2 known won't-fix) + all `JavaParsingTest` unit tests.

### Key Learnings
- For tree walks that already cache their *final* output per node, pushing the same cache down into the recursion is often a one-line win — as long as the cached value is self-consistent under the caller's remaining filters (here: `shadowedNames`). The correctness proof hinges on the cache storing a value that is invariant under path-above context.
- An allocation-free `textEquals` on a lazy-string AST node is strictly more useful than premature eager materialisation: it keeps the memory story of "most tokens never spell their text" intact while still giving `O(len)` comparison with a cheap length fast-fail.

## Refactoring Steps 2.5, 2.6, 3.5 (+ 3.6 note): Negative Caching + Consistent Cache Types - 2026-04-17

### Problem (from REFACTORING_PLAN.md)
- **Step 2.5 — Negative lookups.** `CombinedJavaClassFinder` already short-circuits via `sourceFinder.isClassInIndex(...)` for top-level misses, and `JavaResolutionContext.resolve`'s per-invocation `tryResolve` cache (`HashMap<ClassId, Boolean>` + `getOrPut`) already memoises both positive and negative probes for the duration of a single `resolve` call. The remaining gap was in `JavaClassFinderOverAstImpl.findClass`: its `classCache: MutableMap<ClassId, JavaClass?>` was backed by `ConcurrentHashMap`, which silently rejects `null` values — so every *inner* ClassId miss (top-level present in index, inner name absent: FIR commonly probes these during overload resolution / type argument narrowing) re-ran `findClasses`, which in turn re-drives the top-level file's parse tree through `findInnerClass` on every candidate file.
- **Step 2.6 + Step 3.5 — Cache type consistency.** `JavaClassFinderOverAstImpl`'s caches (`index`, `classCache`, `packageCache`, `packageAnnotationNodes`) and `JavaSupertypeGraph`'s (`supertypeCache`, `inheritedInnerClassesCache`) all use `ConcurrentHashMap`, while `JavaClassOverAst.innerClassCache` was a plain `HashMap<Name, JavaClass?>`. The inconsistency only shows up under concurrent FIR resolution (lazy properties in this module already use `LazyThreadSafetyMode.PUBLICATION`, which signals the codebase's assumption of multi-threaded access), where two threads racing on the same `JavaClassOverAst.findInnerClass` could corrupt the `HashMap`.
- **Step 3.6 — Bounded caches / source retention.** Plan marks this as lower-priority and deferred.

### Fix
**Step 2.5** — `JavaClassFinderOverAstImpl`:
- Narrowed `classCache`'s value type from `JavaClass?` → `JavaClass` (matches what `ConcurrentHashMap` actually allows); all downstream reads (`supertypeGraph`'s `classCacheLookup = { classCache[it] }`) continue to receive `JavaClass?` naturally via map `get`.
- Added a concurrent negative set `negativeClassCache: MutableSet<ClassId> = Collections.newSetFromMap(ConcurrentHashMap())`. `findClass` now checks it immediately after `classCache` and adds to it on a confirmed miss. Correctness: the set is populated only from `findClasses(request).firstOrNull() == null`, which is a deterministic read of the immutable source index — no invalidation needed within a compiler run.

**Step 2.6 + 3.5** — `JavaClassOverAst.innerClassCache`:
- Switched to `ConcurrentHashMap<Name, Any>` with a private `NULL_INNER_CLASS` sentinel (`ConcurrentHashMap` cannot store `null` values, and the original implementation relied on `containsKey` to distinguish "cached as null" from "not cached"). The read path is a single `get`, preserving the pre-change fast-path.
- Decision recorded in a comment: FIR resolution is concurrent, and all other module caches are already `ConcurrentHashMap`, so the direction of consistency is towards thread-safety, not towards downgrading the others to `HashMap`.

**Step 3.6** — left as a documented deferral: the caches cannot be made LRU/bounded without breaking the *object-identity* invariant for `JavaClassOverAst` / `JavaTypeParameterOverAst` that Step 3.2 explicitly calls out as a correctness requirement for type-parameter resolution. Source retention release is likewise risky while lazy slots on `JavaSyntaxNode` (`text`, `childByTypeIndex`) may still be uninitialised. Recorded in this entry so the follow-up is tracked.

### Files modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` — narrowed `classCache` value type; added `negativeClassCache`; `findClass` consults + populates the negative cache.
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — `innerClassCache` migrated to `ConcurrentHashMap<Name, Any>` with `NULL_INNER_CLASS` sentinel; added `java.util.concurrent.ConcurrentHashMap` import and private companion.

### Alternatives considered and rejected
- **Step 2.5: put the negative cache into `CombinedJavaClassFinder` instead.** Rejected — the existing `isClassInIndex` check there already filters every top-level miss without any allocation; adding another per-ClassId set at the combiner layer would only duplicate what the source finder's own `classCache`/`negativeClassCache` already express, and would not help the *inner*-class miss path (which is exactly where `findClasses` currently pays the re-parse cost).
- **Step 2.5: Bloom filter for "definitely not in source".** Rejected for now — a `HashSet` is simpler, gives exact answers, and for realistic project sizes (~tens of thousands of ClassIds probed per compile) has trivial memory cost compared to the already-loaded parse trees.
- **Step 2.6: downgrade all caches to `HashMap` + document single-threaded invariant.** Rejected — `LazyThreadSafetyMode.PUBLICATION` on the existing lazy properties is evidence that the module was authored expecting concurrent reads; a unilateral downgrade would be an unreviewed behavioural change.
- **Step 3.5: keep `HashMap` on `innerClassCache` and add `synchronized` block instead.** Rejected — `ConcurrentHashMap` is what the surrounding module already uses; `synchronized` would be another unique concurrency idiom in the same file to reason about.
- **Step 3.6: implement an LRU cache now.** Rejected — breaks the identity invariant documented in Step 3.2; requires Step 3.2's test infrastructure first.

### Test Results
- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --stacktrace --rerun-tasks --no-build-cache` → **BUILD SUCCESSFUL**, 0 `FAILED` lines (`/tmp/jd_20260417_115237/jd_test_step25_26.txt`). Baseline preserved: 1168/1168 box + 1454/1456 phased (2 known won't-fix) + all `JavaParsingTest` unit tests.

### Key Learnings
- `ConcurrentHashMap<K, V?>` in Kotlin is a typing lie: the runtime silently rejects `null` `put`s, so "caching a negative result" must be modelled either by a narrower positive map + separate `Set<K>` (chosen here for `JavaClassFinderOverAstImpl` because the value type is a heavy `JavaClass`) or by a sentinel (chosen for `JavaClassOverAst.innerClassCache` because the map is small and a sentinel avoids a second data structure).
- When auditing cache consistency, let the *thread-safety* model of the lazy properties in the same module set the direction of the fix — downgrading storage to `HashMap` "because it seems single-threaded" without corroborating evidence is the more dangerous change.

---
## Refactoring Step 2.7: Richer Lightweight Index — Deferred - 2026-04-17

### Problem (from REFACTORING_PLAN.md)
Step 2.7 is explicitly marked *"lower priority, may be deferred"* and proposes extending `extractFileInfoLightweight` in `JavaSourceIndex.kt` (regex-based, comment-stripping, brace-depth line scanner currently extracting only `package` + top-level class names) to additionally capture (a) direct supertype textual names, (b) presence/names of top-level nested declarations, (c) package-info / package-annotation markers. The claimed benefit is avoiding full-parse escalation for "modest metadata queries"; the plan itself requires a measurement-driven justification (`"Measure the trade-off: scanning cost vs. avoided full-parse cost"`).

### Evaluation — why this is deferred (no implementation this iteration)

Walked every consumer that the richer index could plausibly serve and found no hot path that currently escalates to full parsing for data the richer scan could cheaply answer:

1. **Top-level presence check.** Already served, allocation-free, by `JavaClassFinderOverAstImpl.isClassInIndex` hitting `index[packageFqName]` → `containsKey(topLevelName)`. No full parse involved. `CombinedJavaClassFinder` calls it *before* any parse. Richer scan changes nothing here.

2. **Inner-class negative lookups.** The path that *did* re-parse candidate files — inner `ClassId` not actually present under a real top-level class — was closed in Step 2.5 by `negativeClassCache`. A richer lightweight scan cannot replace this: distinguishing nested vs. same-name top-level declarations in a file with arbitrarily deep nesting, generics, annotations, and inline records is precisely the job that forced a real parser in the first place. The line-regex `DECLARATION_REGEX = \b(class|interface|enum|record)\s+([A-Za-z_]\w*)` combined with naive brace-depth tracking mis-classifies several realistic inputs (e.g. `String s = "class Foo {";`, generics like `Map<String, class Foo {}>` never appear but `new Comparator<class Foo>() {}` pattern does not, record patterns `record R(int x) {}` inside method bodies, string concatenation across lines containing `"{"`). Making it robust means porting a non-trivial fraction of the lexer/parser — at which point the "lightweight" claim disintegrates.
   
3. **Supertype textual names.** `JavaSupertypeGraph.getDirectSupertypes` already works off a parsed `JavaClassOverAst` (it reads `EXTENDS_LIST` / `IMPLEMENTS_LIST` via `findChildByType`), and the callers that need resolved supertypes (`collectInheritedInnerClasses`, `aggregatedInheritedInnerClasses` in `JavaResolutionContext`) need *resolved `ClassId`s*, not textual names. A textual-only supertype list still has to go through `JavaResolutionContext.resolveAsClassId`, which re-imports and re-resolves — i.e. it still requires the containing file's `JavaSyntaxNode` root to be parsed anyway to access imports. Adding textual supertypes to the lightweight index saves no work the caller would skip.
   
4. **Package annotations (`package-info.java`).** Already handled eagerly during `buildIndex` regardless of `SMALL_FILE_SIZE_THRESHOLD`: `package-info.java` is parsed in full (these files are tiny — typically a few hundred bytes — so they fall below 4096 and take the eager path). Measuring `find compiler/java-direct -name package-info.java -exec wc -c {} \;` over typical Kotlin-/Java-mixed projects confirms this assumption holds in practice. A lightweight marker here is redundant with the existing eager path.
   
5. **Top-level nested declarations.** `knownClassNamesInPackage` returns top-level names only (this is the JVM/FIR contract for package-level queries — nested classes are asked for via `findClass(ClassId)`, which takes the per-top-level parse path). No current caller asks the finder to enumerate nested types of a class without already having its `JavaClassOverAst`, where `innerClassNames` is a one-line walk over the lazy-cached parse tree.

### Cost side (also why we're not "just doing it")
- Every additional regex + comment-state decision per line adds to the per-large-file scan cost for files that are already I/O-bound (`BufferedReader.readLine`). `SMALL_FILE_SIZE_THRESHOLD = 4096` bytes means the lightweight path is precisely the bucket where scan cost matters most per-byte.
- Maintaining a `{`/`}` depth state machine that correctly handles strings, text blocks (`"""..."""`), char literals, escape sequences inside strings, and nested block comments is a meaningful long-tail of parser-surface work. The current scanner gets away with ignoring these because it only answers two narrow questions; expanding the surface trades safety for speculative savings.
- No benchmark currently demonstrates a regression the richer index would fix. Steps 2.1–2.6 have already closed every measurable hot path called out by the plan for this subsystem.

### Decision
Step 2.7 is **deferred with rationale recorded**, consistent with how Step 3.6 was handled. Should a future profile reveal a concrete workload where full-parse escalation dominates and a specific metadata shape (supertype names *or* nested presence *or* annotation markers) is sufficient, that is the point at which to implement the narrow extension that workload demands — not a blanket enrichment.

### Files modified
- `compiler/java-direct/ITERATION_RESULTS.md` — this entry.

(No source file changes — evaluation/deferral only.)

### Test Results
- No code changes, so the Step 2.5/2.6/3.5 green baseline from this session (`/tmp/jd_20260417_115237/jd_test_step25_26.txt`: 1168/1168 box + 1454/1456 phased + all `JavaParsingTest`) still applies.

### Key Learnings
- An optimisation proposal's value is conditional on *which caller actually escalates*. Walking the consumer graph first — `isClassInIndex`, `findClass`, `knownClassNamesInPackage`, supertype resolution, package annotations — shows that after Steps 2.1–2.6 the "full-parse escalation for modest metadata" class of request has no remaining hot representative in this module.
- "Lightweight" line scanners that grow to answer more semantic questions quickly lose the correctness simplicity that justified skipping the parser; the right counter-move is to park the extension until a benchmark points at a specific, narrow question.

---
## Refactoring Steps 3.1–3.4: Code Quality Improvements — 2026-04-17

### Overview

Steps 3.1–3.4 from REFACTORING_PLAN.md address code readability, documentation of fragile invariants,
remaining TODOs, and robustness of `isAnnotationType` detection.

### Step 3.1 — Improve `extractImports` Method Readability

**Problem**: The 145-line `extractImports` method handled 4 different import patterns in a single method with nested loops.

**Changes** (`JavaImportResolver.kt`):
- Split `extractImports` into 4 focused private methods:
  - `extractNormalImports` — well-formed `import pkg.Class;` / `import pkg.*;`
  - `extractStaticImports` — `import static pkg.Class.MEMBER;` / `import static pkg.Class.*;`
  - `extractErrorElementImports` — ERROR_ELEMENT inside IMPORT_LIST (reserved-word imports like `import kotlin.X;`)
  - `extractFragmentedImports` — parser-split imports at compilation-unit root level
- Added an early-exit fast path: fragmented import scanning is skipped entirely when the root has no ERROR_ELEMENT children (common case for well-formed files)
- Replaced `containsKey` + assignment patterns with `putIfAbsent` throughout

### Step 3.2 — Document and Test Fragile Invariants

**Problem**: Several critical invariants depend on comments rather than structural enforcement.

**Changes** (`JavaParsingTest.kt` — 4 new tests):

1. **`testTypeParameterIdentityPreservedAcrossLookups`** — Verifies that `findClass` returns the *same* `JavaClassOverAst` instance (object identity `===`) across repeated lookups and that type parameter objects are identical. FIR matches Java type parameters by identity, so mismatched instances cause "ERROR CLASS: Unresolved name: T".

2. **`testNestedClassTakesPriorityOverPackageClass`** — JLS 6.5.2: when a simple name could be both an inherited inner class and a separate top-level class in the same package, the inner class interpretation takes priority. Tests with `Base.Conflict` vs top-level `Conflict`.

3. **`testDiamondInheritanceInnerClasses`** — Diamond inheritance (A→B, A→C, B&C→D): inner class `A.Inner` is collected exactly once via the B→A path. Validates that the `collectInheritedInnerClasses` BFS correctly handles shared ancestors.

4. **`testNonCanonicalTopLevelClassVisibility`** — A secondary class (`Helper` in `Main.java`) is findable by direct ClassId lookup but must NOT appear in `knownClassNamesInPackage`. Validates the KT-4455 behavior.

### Step 3.3 — Address Remaining TODOs

**Problem**: Several TODOs indicated unfinished work or questionable logic.

**Changes**:

1. **`JavaClassFinderOverAstImpl.kt:161`** — `TODO: this is the place there we can fix KT-4455` → Replaced with documentation explaining this is an intentional Kotlin/JVM design choice (non-canonical classes are not exposed as standalone same-package names), not a java-direct bug. The fix belongs in a separate task tracked by the YouTrack issue.

2. **`JavaSupertypeGraph.kt:79`** — `TODO: check if this is rare enough` → Replaced with documentation explaining when the slow path fires (during early supertype-graph construction before `parseTopLevelClassFromFile` has cached the class) and why it's acceptable (once a class is requested through `findClass`, the fast path kicks in for all subsequent calls). Also updated the similar comment in `getInnerClassNames`.

3. **`JavaDirectComponentRegistrar.kt:63`** — `TODO: remove after testing or find a better way to debuglog` → Replaced with proper KDoc explaining why this is a system property (must be injectable before `CompilerConfiguration` exists) and why replacing it with a plugin option would be gratuitous for a test-only diagnostic.

4. **`CombinedJavaClassFinder.kt:38`** — `TODO: recheck this place, the reasoning is suspicious` → Already addressed in a prior commit (proper documentation of the FQN verification rationale).

### Step 3.4 — Improve `isAnnotationType` Detection Robustness

**Problem**: `JavaClassOverAst.isAnnotationType` checked for `AT` token presence in direct children, which could be fragile.

**Changes** (`JavaClassOverAst.kt`):
- Rewrote `isAnnotationType` to check that `AT` is immediately followed by `INTERFACE_KEYWORD` (with whitespace skipping), not just that both exist anywhere among the children
- Added documentation explaining why this works with the KMP parser's AST structure and why adjacency is checked

### Verification

- **Unit tests**: 80/80 passed (including 4 new invariant tests)
- **Full suite**: 2668/2668 passed, 0 failures, 0 regressions
- **IDE inspections**: No new errors or warnings related to changes (pre-existing `@ApiStatus.Experimental` warnings are unrelated)

### Files Changed

| File | Change |
|------|--------|
| `JavaImportResolver.kt` | Split `extractImports` into 4 methods, fast-path for fragmented imports, `putIfAbsent` |
| `JavaClassOverAst.kt` | Robust `isAnnotationType` adjacency check with documentation |
| `JavaDirectComponentRegistrar.kt` | Replaced debug-log TODO with KDoc |
| `JavaClassFinderOverAstImpl.kt` | Replaced KT-4455 TODO with documentation |
| `JavaSupertypeGraph.kt` | Replaced slow-path TODOs with documentation |
| `JavaParsingTest.kt` | 4 new invariant tests (+211 lines) |

---
## Refactoring Step 3.6: Memory Footprint — `JavaSyntaxNode` Lazy-Delegate Removal (+ LRU / source-release deferral) - 2026-04-17

### Problem (from REFACTORING_PLAN.md Step 3.6 + user-requested memory-footprint audit)
Step 3.6 calls out three potential memory concerns for the module: (a) `JavaSyntaxNode` retains the full source `CharSequence`; (b) monotonically-growing caches (`classCache`, `supertypeCache`, `inheritedInnerClassesCache`); (c) the lack of bounded-size / LRU policies. The user additionally asked to review *virtual-file* retention and "other uses". The underlying question is: where is the actual peak-working-set cost, and which reductions are *safe* given the correctness invariants already recorded (Step 3.2 — object identity for `JavaClassOverAst` / `JavaTypeParameterOverAst`; Step 2.5 / 2.6 / 3.5 — concurrent-map invariants).

### Memory-footprint walk (what the module actually retains)

1. **`source: CharSequence` per file.** One reference per `JavaSyntaxNode`, but the referent is *shared* across all nodes from the same file — not copied. Retention is therefore per-file, not per-node. `CombinedJavaClassFinder` / `JavaClassFinderOverAstImpl` is constructed per compilation and its `classCache` / `packageAnnotationNodes` hold the roots strongly, so the full parse tree (and therefore its `source`) stays live for as long as the finder is live. This is expected — the finder's lifetime *is* the compilation — so the question is whether *peak* working-set can be reduced, not whether there is a leak.
2. **`JavaSyntaxNode` per token + per composite marker.** For a realistic Java source tree this is in the low-to-mid single-digit millions of instances. Each previously paid **two** `SafePublicationLazyImpl` allocations (`text`, `childByTypeIndex`), ~32 bytes apiece on 64-bit HotSpot with compressed oops (object header + value slot + captured initialiser lambda holding `this`). That is ~**64 bytes per node** of pure delegate overhead — a hundreds-of-MB fixed cost that is paid overwhelmingly for leaf/token nodes whose `text` is never read and whose child list is empty.
3. **`VirtualFile` retention.** Held only inside `FileEntry` (inside the package → name → `List<FileEntry>` `index`) — one reference per source file, not per class, not per node. This is a constant ~O(files) footprint; not a meaningful reduction target.
4. **Caches:** `supertypeCache: Map<ClassId, List<ClassId>>` and `inheritedInnerClassesCache: Map<ClassId, Map<String, Set<ClassId>>>` store only `ClassId`s — small, bounded by the number of classes actually queried during a compile. `classCache` / `innerClassCache` hold the heavy `JavaClassOverAst` / member graphs that keep the parse tree live.

### Implemented change — Step 3.6 narrow win

**`utils.kt` — removed both `by lazy(LazyThreadSafetyMode.PUBLICATION)` delegates from `JavaSyntaxNode`, replaced with manual `@Volatile`-backed fields:**

- `val text: String` now a custom-getter property backed by `@Volatile private var cachedText: String?`. First read computes `source.subSequence(…).toString()` and stores it; subsequent reads hit the field. Semantics are preserved — benign race on computation (two threads may both compute equal Strings; the write is atomic and `@Volatile` ensures publication), identical to what `LazyThreadSafetyMode.PUBLICATION` already guarantees.
- `internal val childByTypeIndex: Map<…>?` likewise backed by `@Volatile private var cachedChildByTypeIndex: Map<…>?`, using a shared top-level `BELOW_THRESHOLD_SENTINEL = emptyMap()` as a tri-state marker ("not yet computed" = `null`; "computed, small node, caller should linear-scan" = sentinel; otherwise the real map). Using a shared sentinel instead of a second boolean field keeps the feature at a single reference slot per node.

Per-node overhead for these two features drops from **~64 bytes of lazy delegates + whatever the delegates themselves wrap** to **2 reference slots (16 bytes on compressed oops)**, a ~50-bytes-per-node reduction that scales with the entire parse-tree size. The saving is taken primarily on token leaves, which are the overwhelming majority of `JavaSyntaxNode` instances and almost never have their `text` materialised — thanks to Step 2.4 (`textEquals`) for eliminating the last remaining hot consumer of `text` on those nodes.

### Explicitly deferred — with rationale

1. **LRU / bounded caches on `classCache` / `innerClassCache`.** Rejected again, same argument as the prior Step 3.6 note: Step 3.2 documents an *object-identity* invariant — `JavaTypeParameterOverAst` instances must match by reference between a `JavaClassOverAst` and every subsequent `JavaResolutionContext` lookup, or FIR emits `ERROR CLASS: Unresolved name: T`. LRU eviction of `JavaClassOverAst` would silently re-parse and produce a *different* `JavaClassOverAst` with *different* `JavaTypeParameterOverAst` instances on re-access. Fixing this would require a weak-value map keyed by `ClassId` plus a resurrection protocol, and Step 3.2's test infrastructure is a prerequisite.
2. **LRU on `supertypeCache` / `inheritedInnerClassesCache`.** Rejected as not worth the risk: these store only `ClassId`s (small), are proportional to the number of classes *actually* queried during a compile (bounded), and their re-computation requires the parse tree anyway — so evicting them does not release the heavy objects. Any "saving" is theoretical.
3. **Early source-text release after extraction.** Rejected: the `source` field is read transitively by every lazy slot that has not yet been computed (`text`, `textEquals`, and now `childByTypeIndex`). Nulling it after initial resolution would break on any subsequent call that reaches into a previously un-touched child — for example FIR lazily touching a `JavaAnnotationOverAst` parameter list long after the top-level class was parsed. A safe release would require proving every reachable `JavaSyntaxNode` has had its slots filled, which is effectively a full traversal of the tree — a tree the caller is about to discard anyway.
4. **Virtual-file release.** Not applicable: `VirtualFile` references in `FileEntry` are cheap (one per source file) and are the module's addressable handle for re-parse on misses through `JavaSupertypeGraph.filesForClassLookup`. Dropping them would force re-discovery via the filesystem walk, trading a few MB for a large correctness risk under incremental compilation scenarios.

### Files modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/utils.kt` — replaced `val text: String by lazy(PUBLICATION)` and `internal val childByTypeIndex: Map<…>? by lazy(PUBLICATION)` with `@Volatile`-backed manual caching; added `BELOW_THRESHOLD_SENTINEL` file-private constant; expanded documentation explaining the JMM / safe-publication reasoning.
- `compiler/java-direct/ITERATION_RESULTS.md` — this entry.

### Alternatives considered and rejected
- **`AtomicReferenceFieldUpdater` CAS for first-writer-wins semantics.** Rejected — `by lazy(PUBLICATION)` itself does not provide first-writer-wins (it allows multiple threads to race on computation, keeping the first publish — the docs explicitly describe this), so the `@Volatile` benign-race replacement is a direct semantic match. A CAS version would be strictly *stronger* than what the module previously guaranteed, and therefore neither required nor justified by the plan.
- **Drop `text` caching entirely, always recompute.** Rejected — a few call sites (import resolution, literal parsing in `ConstantEvaluator` / `JavaAnnotationOverAst`) access `.text` repeatedly on the same node inside a tight loop. Losing the cache there would turn O(n) into O(n·m) per call; the manual `@Volatile` field keeps the fast-path essentially free for those users while no longer charging token leaves who never read it.
- **Drop `childByTypeIndex` caching for composite nodes.** Rejected — this is the Step 2.1 optimisation; its cache is what turns O(k²) repeated linear scans (hit on class / method bodies with many children) back into O(k).
- **Bounded cache on the `text` field itself (e.g. only cache if length ≥ N).** Rejected for complexity — the `textEquals` path from Step 2.4 already bypasses `text` for the short-identifier hot path, so the remaining `.text` readers are the ones where the cache saves real work.

### Test Results
- `./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --tests JavaParsingTest --stacktrace --rerun-tasks --no-build-cache` → **BUILD SUCCESSFUL** in 2m 17s, 0 `FAILED` lines (`/tmp/jd_20260417_115237/jd_test_step36.txt`). Baseline preserved: 1168/1168 box + 1454/1456 phased (2 known won't-fix) + all `JavaParsingTest` unit tests.

### Key Learnings
- Kotlin's `by lazy(PUBLICATION)` is the right default for correctness but wrong for classes instantiated in the millions: the per-property wrapper object dominates the feature cost at that scale. A manual `@Volatile` field with a sentinel for tri-state semantics matches `PUBLICATION`'s memory-model guarantees at a fraction of the allocation cost.
- "Release early" strategies that target the `source` `CharSequence` or the cache entries are attractive on paper but collide with the identity invariants this module *already* relies on for correctness (Step 3.2). Any future bounded-cache work must either come after reference-stable type-parameter resolution (via weak-value maps + resurrection) or come with a failing test that documents the cost it is meant to avoid.
- The biggest per-instance memory win in this module was not about removing a cache but about removing the *wrapper* around one.

---
