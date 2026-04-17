# Java-Direct: Iteration Results Log

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Last Updated**: 2026-04-17 (refactoring step 1.6 — split `JavaClassFinderOverAstImpl`)

---

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

---

## Refactoring Step 1.5 (follow-up): Remove IO try/catch and logging - 2026-04-17

Per review feedback, IO error handling belongs at a higher compiler level — the reader should not
catch and log `IOException` itself. Removed all `try`/`catch` blocks from
`DefaultJavaSourceFileReader` (`readFileContent`, `openLineReader`, `walkSourceRoots.walk`) along
with the now-unused `java.util.logging.Logger`/`Level` and `java.io.IOException` imports. The
reader still distinguishes "not a readable regular file" (silent `null` for invalid/directory) from
a real read attempt; any `IOException` from the VFS now propagates to the caller. Tests green:
baseline 1168/1168 box + 1454/1456 phased preserved.

---

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

---

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

---

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

---

## Archives

| Archive | Iterations | Result |
|---------|------------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1–6 | 0 → 90/138 box (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7–16 | 90 → 1075/1166 box (92.2%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17–23 | 1075 → 1150/1167 box, 1374/1442 phased (95.3%) |
| `implDocs/archive/ITERATIONS_24_26_DETAILS.md` | 24–26 | 1150/1167 → same, phased 300 → 1374/1442 |
| `implDocs/archive/ITERATIONS_27_36_DETAILS.md` | 27–36 | 1150/1167 → 1157/1168 box, **79 combined failing** |
| `implDocs/archive/ITERATIONS_37_51_DETAILS.md` | 37–51 | 1157/1168 → 1165/1168 box, **17 combined failing** |
| `implDocs/archive/ITERATIONS_52_71_DETAILS.md` | 52–71 | 1165/1168 → 1168/1168 box, 1454/1456 phased, **2 won't-fix**; perf + refactoring |

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
