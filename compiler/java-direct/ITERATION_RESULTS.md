# Java-Direct: Iteration Results Log

**Current status**: `:compiler:java-direct:test` full suite green, 2839/2839 (100%). No known won't-fix.

**Last archived**: `implDocs/archive/ITERATION_RESULTS_2026_07_13.md` (entries through 2026-07-13).

---

## How to write entries

This log is read into the agent's context every session, so **entries must stay short**.

- **Newest entry on top.** One entry per landed change or per investigated regression.
- **Cap each entry at ~15 lines / ~150 words.** If the rationale, a trace, or a
  measurement table is longer, put it in a dedicated `implDocs/<TOPIC>.md` and link to it
  from the entry — do not inline it here.
- **Use the fixed fields below.** No free-form multi-paragraph narration; if a field needs
  more than ~2 lines, link out instead.
- **No pasted logs, stacktraces, or diffs.** Quote the single line that matters; link the rest.
- **Archive when this file passes ~600 lines** (see `AGENT_INSTRUCTIONS.md` →
  "Docs Maintenance"): `git mv` it to
  `implDocs/archive/ITERATION_RESULTS_<last-entry-date>.md`, add an archive banner, and
  reset this file to the template below.

### Entry template

```
### YYYY-MM-DD — <one-line title>
- **Change**: what changed and why (1–3 lines).
- **Files**: key files touched (+N/−M LoC if useful).
- **Tests**: suites run + counts (e.g. box 1178/1178, phased 1513/1513).
- **Result**: green / regression fixed / won't-fix — link to a detail doc if there is one.
```

---

<!-- Add new entries below, newest first. -->

### 2026-07-29 — Re-derive the KT-74097 guard rationale after enum-entry annotations went lazy
- **Change**: re-traced whether the lazy Java annotation lists retire any cycle breaker. They do
  not: they removed the only known crashing trigger (`@Deprecated` enum constant,
  `testIntellij_vcs_git`), demoting `cycleSafeClassLikeSymbol` to genuine defense-in-depth, but the
  cycle class stays reachable — the `declarations` lazy reads `FirJavaClass.typeParameters`, whose
  bound enhancement iterates the *class's own* annotations via `extractDefaultQualifiers` (plus a raw
  outer-class `getClassLikeSymbolByClassId`); the enum-entry `returnTypeRef` is still eager because
  `SignatureEnhancement` requires a resolved ref; and 3 of 5 guard call sites carry no annotation
  (const-field values, `@Target`, type-argument substitution). The other breakers
  (`cycleGuardedSupertypeWalk`, supertype memoization, local `visited` sets) are annotation-agnostic.
  Docs/comments updated accordingly; also fixed the stale `JavaSupertypeLoopChecker` name (the code
  is `cycleGuardedSupertypeWalk` / `JavaModelSupertypeWalkGuard`).
- **Files**: `resolution/JavaModelSessionAccess.kt`, `test/.../JavaCycleBreakerTest.kt` (comments
  only), `AGENT_INSTRUCTIONS.md`.
- **Tests**: not run — comment/docs-only, verified via `git diff` that no code line changed.
- **Result**: green (no code changes).

### 2026-07-29 — Clamp the lightweight scanner's brace/paren balance at zero
- **Change**: review of `extractFileInfoLightweight` against its production ancestor
  `SingleJavaFileRootsIndex.JavaSourceClassIdReader` (cli-base). Unmatched closers drove the
  balances negative, so `atTypeDeclaration()` (`== 0`) stopped firing and every top-level type
  after a stray `}`/`)` was lost — for a stray `}` before the file's namesake class the file is
  dropped from the index entirely (`tryBuildFileEntry` requires the base name). Now clamped,
  mirroring `Kotlin.flex`'s `if (lBraceCount == 0) popState() else lBraceCount--`; no-op on
  well-formed input. Also dropped a dangling KDoc, the redundant `if (at(SEMICOLON)) advance()`
  (the class-scan loop skips it anyway) and mapped an empty package name to `null`.
  Divergences from the ancestor are intentional and stay: the `break@loop` on a non-`package`
  token (upstream appends the first class name to the package on a missing `;`), no `isPackageInfo`
  arm (`JavaPackageIndexer` routes `package-info.java` to `JavaPackageInfoIndexer` first), and
  `when (lexer.getTokenType())` instead of four `at()` calls. `getTokenType()` itself is a cached
  field read (`JavaLexer.locateToken`: `if (myTokenType != null) return`), so repeated calls need
  no hoisting; `getTokenText()` is the allocating one, and it is only reached at balance 0.
- **Files**: `util/JavaSourceIndex.kt` (+7/−12); `JavaParsingLightweightScannerTest.kt` (+47, 2 tests).
- **Tests**: `JavaParsing*` 16/16; box+phased 2792 executed / 0 FAILED. Both new tests fail
  without the clamp (`got [Broken]`, `got [Foo]`).
- **Result**: green (error-tolerance fix).

### 2026-07-29 — Delete the `JavaSourceFileReader` abstraction; read via `File.readText`
- **Change**: after the `VirtualFile`→`File` switch the interface had a single implementation
  (`DefaultJavaSourceFileReader`), `walkSourceRoots` had no callers (it served the deleted eager
  `buildIndex`), and no production or test call site ever substituted a reader — the parameter was
  threaded through four collaborators for nothing. Replaced by one internal
  `readJavaSourceFileText(File): String?`; the reader parameter is gone from `JavaClassCache`,
  `JavaPackageIndexer`, `JavaPackageInfoIndexer`, `JavaSupertypeGraph`,
  `JavaClassFinderOverAstImpl` and `extractFileInfoLightweight`. Content is now read with
  `File.readText()` instead of `String(readBytes(), UTF_8)`: measured over 1503 repo `.java` files
  (37 MB, interleaved rounds, 7 samples) the medians are 46 ms vs 48 ms — ~2 ms of pure read time
  per full corpus, i.e. negligible against lexing/parsing.
- **Files**: deleted `util/JavaSourceFileReader.kt` (−57), added `util/javaSourceFileText.kt` (+19);
  `JavaClassCache.kt`, `JavaClassFinderOverAstImpl.kt`, `JavaPackageIndexer.kt`,
  `JavaPackageInfoIndexer.kt`, `util/JavaSourceIndex.kt`, `util/JavaSupertypeGraph.kt`; tests
  `JavaParsingTestBase.kt`, `JavaParsingLightweightScannerTest.kt`; `implDocs/ARCHITECTURE.md`.
- **Tests**: `JavaParsing*` green; box+phased 2790 executed / 0 FAILED.
- **Result**: green (behaviour-preserving simplification).

### 2026-07-28 — Replace `FirJavaEnumEntry` with `buildEnumEntry` + lazy `MutableList` annotations
- **Change**: review follow-up on the 2026-07-21 KT-74097 fix — the hand-written `FirJavaEnumEntry`
  duplicated `FirEnumEntryImpl` (~180 LoC) just to host a lazy annotation slot. The tree generator
  gained a per-field opt-in (`LeafBuilder.listFieldsWithVar` + `useVarForListField` DSL in
  `AbstractBuilderConfigurator`; configured only for `builder(enumEntry)` in the FIR
  `BuilderConfigurator`), so default builder generation is unchanged and only
  `FirEnumEntryBuilder.annotations` becomes `var`. The enum-entry arm of
  `convertJavaFieldToFir` uses plain `buildEnumEntry` with a new
  `FirLazyJavaAnnotationMutableList` — an `AbstractMutableList` composing a plain
  `FirLazyJavaAnnotationList` (conversion reused via `toMutableList()` on first mutable access;
  only the 5 abstract members are overridden; cheap `isEmpty` keeps `toMutableOrEmpty()` from
  forcing conversion) — and `FirJavaLazyDeprecationsProvider`. `FirJavaEnumEntry` deleted.
- **Files**: `generators/tree-generator-common/.../Builder.kt`, `AbstractBuilderPrinter.kt`,
  `config/AbstractBuilderConfigurator.kt`, `fir/tree/tree-generator/.../BuilderConfigurator.kt`,
  regenerated `fir/tree/gen/.../builder/FirEnumEntryBuilder.kt` (single-line `val`→`var`),
  `fir-jvm/.../FirJavaFacade.kt`, `FirJavaAnnotationList.kt`, `javaAnnotationsMapping.kt`;
  deleted `FirJavaEnumEntry.kt`.
- **Tests**: box+phased green (0 FAILED); PSI gate + `CompileKotlinAgainstKotlin` gate green
  (shared fir-jvm + fir-tree edits).
- **Result**: green (behaviour-preserving simplification; laziness retained).

### 2026-07-28 — Comment-style cleanup of the binary/source-divide branch
- **Change**: rewrote/deleted LLM-verbose comments added since the divide commit (added
  comment lines ~290 → ~125): dropped restatements of obvious code, caller inventories,
  counterfactual "rather than" phrasing, per-`@param` chatter, `Stage`/`§`/`implDocs`
  references in shared `fir-jvm` sources; kept short why-notes, API contracts, and
  regression/testData guards. Codified the rules in `AGENT_INSTRUCTIONS.md` →
  *Source Comment Conventions* ("default is no comment" gate, ~3% density baseline).
- **Files**: comment-only edits across ~20 files in `cli-base`, `cli-jvm`, `fir-jvm`,
  `frontend.common.jvm`, `core/compiler.common.jvm`, `java-direct/{src,test}`; docs:
  `AGENT_INSTRUCTIONS.md` (conventions rewrite, reference-table refresh, stale status header).
- **Tests**: not run — comment-only change, verified via `git diff` that no code line changed.
- **Result**: green (no code changes).

### 2026-07-27 — Make Java-source package directory descent case-sensitive
- **Change**: `JavaPackageIndexer`'s per-package directory descent used `File(dir, segment).isDirectory`
  (added 2026-07-21 when the source path moved from `VirtualFile` to `java.io.File`), which is
  case-insensitive on macOS/Windows. A sibling source dir (`syntax/logger`, `platform/ml/session`)
  was wrongly accepted as package `Logger`/`Session`, so nested-class imports like
  `com.intellij.platform.syntax.Logger.Attachment` mis-split into a package prefix and reported
  `UNRESOLVED_IMPORT`. Descent now matches against the parent's real child names via `File.list()`
  (case-sensitive), mirroring the binary index / PSI VFS `findChild`.
- **Files**: `JavaPackageIndexer.kt` (new `descendDirectoriesCaseSensitive`, used by
  `findPackageDirectories` + `findPackageDirectoryUnder`).
- **Tests**: box+phased green (0 FAILED); `IntelliJFullPipelineTestsGenerated.testIntellij_platform_syntax`
  and `testIntellij_platform_ml` now pass with java-direct on.
- **Result**: regression fixed.

### 2026-07-22 — Gate the binary seam on `useJavaDirect`; delete dead finder; dedup the ASM binary reader
- **Change**: applied `implDocs/BINARY_SOURCE_DIVIDE_REVIEW_2026_07_22.md` §4.1/§4.2/§4.7.
  §4.1: the binary deserializer seam is now gated on `configuration.useJavaDirect` in
  `prepareJvmSessions` — ON uses `JvmBinaryClassFinderInputsOverIndex`, OFF returns `null` so the
  deserializer falls back to the PSI `FirJavaFacade` binary reader (both source and binary now share
  one flag). §4.2: removed dead `CombinedJavaClassFinder.kt` (no references). §4.7: extracted the
  shared `readBinaryJavaClass` core (caching + inner-class dispatch + `ClassifierResolutionContext`)
  into `frontend.common.jvm`; both `JvmBinaryClassFinderInputsOverIndex` and the binary branch of
  `KotlinCliJavaFileManagerImpl` delegate to it. Investigated §4.1's "turn off PSI finder creation":
  not doable now — `KotlinCliJavaFileManagerImpl` still backs JPMS `module-info` resolution
  (`ClasspathRootsResolver` → `JavaModuleInfo.read` → `findClass`) regardless of the flag, and its
  PSI class-loading branch is already inert by default (`usePsiClassFilesReading=false`).
- **Files**: `cli-jvm/.../JvmFrontendPipelinePhase.kt`, `cli-base/.../KotlinCliJavaFileManagerImpl.kt`,
  `frontend.common.jvm/.../classFiles/BinaryJavaClassReader.kt` (new),
  `JvmBinaryClassFinderInputsOverIndex.kt`; deleted `CombinedJavaClassFinder.kt`.
- **Tests**: box+phased green (0 FAILED); PSI (`PhasedJvmDiagnosticLightTreeTestGenerated`) 0 fail,
  `CompileKotlinAgainstKotlin` 0 fail (shared-pipeline + file-manager edits), `KotlinCliJavaFileManagerTest` 7/7.
- **Result**: green (behaviour-preserving refactor + flag-gated seam).

### 2026-07-21 — Thread `java.io.File` through the Java-source indexing path (drop internal `VirtualFile`)
- **Change**: the module no longer relies on `com.intellij.openapi.vfs.VirtualFile` for its own
  source-file representation/reading; source roots are consumed as `java.io.File` (the CLI's
  `JavaSourceRoot.file` is already a `File`, so the old VFS-resolution step is removed). Per-package
  directory descent uses `File(dir, segment)`/`listFiles()`; content is read via `readBytes()`
  decoded as UTF-8 (unchanged charset). Binary-class-finder/CLI wiring stays on `VirtualFile`
  intentionally (external contract).
- **Files**: `JavaDirectFacadeBuilder.kt`, `JavaPackageIndexer.kt`, `JavaPackageInfoIndexer.kt`,
  `util/JavaSourceFileReader.kt`, `util/JavaSourceIndex.kt`; tests `JavaParsingTestBase.kt`,
  `JavaParsingClassFinderTest.kt`, `JavaParsingLightweightScannerTest.kt`.
- **Tests**: box+phased green (0 FAILED); `JavaParsing*` green.
- **Result**: green. Watch-point: a missing source root now drops later in the pipeline
  (`isDirectory`/`isFile`) rather than at VFS resolution — equivalent end behaviour.

### 2026-07-21 — Defer Java enum-entry annotations via `FirLazyJavaAnnotationList` (KT-74097)
- **Change**: the enum-entry arm of `convertJavaFieldToFir` resolved annotations eagerly while
  materialising `FirJavaClass.declarations`, which could re-enter the same in-flight `ClassId`
  (self-cycle). New `FirJavaEnumEntry` (mirrors `FirJavaField`) backs `annotations`/
  `deprecationsProvider` with `FirLazyJavaAnnotationList`, so no eager resolution happens while
  `declarations` is built. Removed the now-dead `setAnnotationsFromJava`; the
  `cycleSafeClassLikeSymbol` guard is now defense-in-depth, not the sole crash preventer.
- **Files**: `fir-jvm/.../declarations/FirJavaEnumEntry.kt` (new), `fir-jvm/.../FirJavaFacade.kt`,
  `fir-jvm/.../javaAnnotationsMapping.kt`, `JavaCycleBreakerTest.kt` (comment).
- **Tests**: box+phased green; PSI (`PhasedJvmDiagnosticLightTreeTestGenerated`) +
  `CompileKotlinAgainstKotlin` gates green (shared fir-jvm edit).
- **Result**: green.

### 2026-07-21 — Reuse AST name extraction in `JavaSupertypeGraph` (drop `splitCanonicalFqName`)
- **Change**: supertype-reference name splitting no longer re-implements type-resolution via the
  generic-bracket-aware `splitCanonicalFqName` text scan; it reuses the AST-based
  `extractReferenceNameParts` (extracted from `JavaClassifierTypeOverAst` into `JavaTypeOverAst`),
  reading `JAVA_CODE_REFERENCE` identifier segments directly. The generic-argument edge case
  (`a.B<String>.C`) is preserved.
- **Files**: `util/JavaSupertypeGraph.kt`, `model/JavaTypeOverAst.kt`.
- **Tests**: box+phased green (0 FAILED); `JavaParsing*` green.
- **Result**: green.

### 2026-07-21 — Scan Java-lexer tokens in `JavaSourceIndex`; exclude comment/bad tokens from the light-tree root
- **Change**: `extractFileInfoLightweight` scans the Java-lexer token stream
  (`JavaSyntaxDefinition.createLexer`) instead of regex/comment-stripping to find the package name
  and top-level type names, using brace/paren balance for nesting; removed
  `PACKAGE_REGEX`/`DECLARATION_REGEX`, manual comment stripping, and the now-unused reader
  `openLineReader`. `JavaLightTree` synthetic-root children now also exclude comments (root-only,
  each declaration keeps its `DOC_COMMENT` for `@deprecated`) and `BAD_CHARACTER`.
- **Files**: `util/JavaSourceIndex.kt`, `util/JavaSourceFileReader.kt`, `parse/JavaLightTree.kt`.
- **Tests**: `JavaParsing*` (incl. lightweight scanner) green; box+phased green (0 FAILED).
- **Result**: green.

### 2026-07-20 — Perf review: memoize recomputed reads in the Java-source model
- **Change**: the model layer recomputed pure, AST-derived values on every access. Converted the
  hot ones to `by lazy(PUBLICATION)` (same precedent as `supertypes`/`typeParameters`): class
  keyword flags (`isInterface`/`isEnum`/`isRecord`/`isAnnotationType`/`isSealed`),
  `methods`/`fields`/`constructors`/`recordComponents`/`innerClassNames`/`annotations`; per-member
  `resolutionContext`/`valueParameters`/`returnType` and field
  `leadingFieldNode`/`modifierList`/`type`/`initializerNode`/`annotations`; type
  `rawTypeNameParts`/`typeArguments`. Memoizing the class collections is the key enabler — member
  wrappers are now stable, so the per-member lazies actually cache. Behaviour-preserving (pure
  functions of the immutable AST + already-lazy `classifier`).
- **Files**: `model/JavaClassOverAst.kt`, `model/JavaMemberOverAst.kt`, `model/JavaTypeOverAst.kt`.
- **Tests**: box+phased 2767/2767 (0 FAILED); `JavaParsing*` 105/105.
- **Result**: green. Full write-up + reviewed-healthy caches + riskier follow-ups (plain-`HashMap`
  concurrency in `JvmBinaryClassFinderInputsOverIndex`, annotation `classId` memoization, more
  per-type/param lazies) in `implDocs/PERFORMANCE_REVIEW_2026_07_20.md`.

### 2026-07-20 — Read package-level default-nullability annotations off binary `package-info.class`
- **Change**: the library-session facade's finder (was a no-op `findPackage`) now materialises a
  binary `<pkg>/package-info.class` and exposes its class-level annotations as the package's
  `JavaPackage.annotations`. Previously those were dropped, so JSR-305/JSpecify package defaults
  (`@ParametersAreNonnullByDefault`, `@TypeQualifierDefault`, `@NullMarked`, …) on a **binary**
  Java package were invisible: a type-variable parameter substituted with an explicitly nullable
  Kotlin type argument stayed nullable instead of becoming definitely-non-null, producing a
  spurious `UNSAFE_CALL` in user code (dokka's `Property<File?>.map { it.relativeToOrSelf(..) }`).
  The finder reuses the same memoised binary index the deserializer reads through; class/package
  existence still routes through the deserializer, so only annotations are added.
- **Files**: `JvmBinaryClassFinderInputsOverIndex.kt` (+`findPackageInfoClass`),
  `JavaDirectFacadeBuilder.kt` (`NoOpJavaClassFinder` → `LibraryJavaClassFinder` +
  `BinaryPackageInfoJavaPackage`), `cli-jvm/…/JvmFrontendPipelinePhase.kt` (thread the shared
  binary-inputs builder into the facade builder); new test
  `codegen/boxJvm/javaInterop/foreignAnnotationsTests/tests/dnnParameterFromBinaryPackageAnnotation.kt`.
- **Tests**: full box+phased suite green (0 FAILED). New reproducer fails without the fix with the
  exact reported symptom (`UNSAFE_CALL … nullable receiver of type 'String?'`) and passes with it.
- **Result**: regression fixed (java-direct now matches PSI on binary package defaults).

### 2026-07-16 — Remove the loose `probeFqnSplits` fallback: commit to the leftmost type like javac
- **Change**: `resolveQualifiedNameToClassIdFromParts` no longer retries a failed name as a plain
  `package.Class` split. Like javac (JLS 6.5.4/6.5.5), once a leftmost type is found the
  interpretation is committed: a failed member-type descent returns the *nonexistent* nested id of
  the committed prefix (full resolution), which stays unresolved downstream — red code, exactly as
  javac reports on a package/type name clash (JLS 6.1). The reentrance-safe flavor returns `null`
  instead, so supertype-walk seeding is never poisoned by a dangling id. `probeFqnSplits` deleted.
- **Tests**: the strict behavior conflicts with the PSI Java model (which loosely resolves the
  package interpretation), so the two tests pinning the loose behavior moved out of the shared
  roots: `qualifiedNamePackageClassClash.kt` deleted from the shared roots and recreated with
  javac-strict expectations in the new java-direct-owned `testData/diagnostics` root (wired into
  `TestGenerator`); the pre-existing `javac/qualifiedExpression/PackageVsClass2.kt` — verified
  against real javac to be red code ("cannot find symbol: class b, location: class a") — is
  skipped via the new `SkipTestsPinningPsiJavaModelDeviationsMetaConfigurator` and mirrored
  strictly in the same root. Strict diagnostics: `MISSING_DEPENDENCY_CLASS` on the call whose Java
  signature uses the clash name + `UNRESOLVED_REFERENCE` on members of the unresolved type.
- **Files**: `resolution/JavaTypeResolver.kt` (−32/+18), `testFixtures/…/components.kt`,
  `testFixtures/…/AbstractJavaUsingAstTest.kt`, `testFixtures/…/TestGenerator.kt`,
  `build.gradle.kts` (own testdata root registered); Scenario D refreshed in `ReadMe.md` and
  `implDocs/RESOLUTION_SCHEMA.md`.
- **Result**: the module is now javac-conformant on qualified-name resolution — the last
  deliberate JLS deviation (KT-87813's unsound loose fallback) is gone. Full module suite green
  (box + phased, 0 FAILED; the skipped PSI-pinning test is mirrored strictly).

### 2026-07-16 — Rewrite `resolveQualifiedNameToClassIdFromParts` as a left-to-right JLS 6.5.4 pass
- **Change**: replaced the recursive try-every-split loop (outer-prefix enumeration + per-prefix
  recursion, O(n²) probes) with a single non-recursive left-to-right pass that mirrors javac's
  PackageOrTypeName classification: first segment as a simple type name in scope (JLS 6.5.4.1),
  else grow the package prefix until a segment names a top-level type (JLS 6.5.4.2), then a
  member-type descent probing declared-then-inherited at every segment (JLS 6.5.5.2). The loose
  `probeFqnSplits` fallback survives, but is now reached only when the JLS pass fails — the sole
  divergence from javac remains the package/type name clash pinned by
  `qualifiedNamePackageClassClash.kt` (KT-87813).
- **Files**: `resolution/JavaTypeResolver.kt` (−36/+28); Scenario D refreshed in `ReadMe.md` and
  `implDocs/RESOLUTION_SCHEMA.md`.
- **Tests**: box+phased suite green (0 FAILED); `JavaParsing*` unit tests green.
- **Result**: simplification landed (behavior-preserving on the whole suite).

### 2026-07-15 — Drop redundant `JavaToKotlinClassMap` disjunct in `resolveFromJavaLang`
- **Change**: `resolveFromJavaLang` accepted a name when either `JavaToKotlinClassMap.mapJavaToKotlin`
  hit **or** `classExists` was true; the map disjunct is dead. It only ever probes `ClassId(java.lang, X)`,
  and `classExists` resolves those via the symbol provider whose JVM builtins arm answers only `kotlin.*`
  ids (`StandardClassIds.builtInsPackages`), so a `java.lang.*` lookup never returns a `BuiltIns`-origin
  symbol filtered by `tryResolve` — it hits the JDK library class instead. Every mapped `java.lang` fqName
  (Object/String/Number/CharSequence/Comparable/Throwable/Cloneable/Iterable/Enum/wrappers/Deprecated)
  exists in both full JDK and mockJDK, so `classExists` already covers exactly what the map would.
  Collapsed to `classExists(classId, fullResolution)`, matching `resolveFromSamePackage`; better PSI
  parity (no divergence in the no-JDK case). Removed the now-unused `JavaToKotlinClassMap` import.
- **Files**: `resolution/JavaTypeResolver.kt` (−5/+1).
- **Tests**: box+phased suite green (0 FAILED/0 errors).
- **Result**: simplification landed (reviewer question — `classExists` alone suffices).

### 2026-07-15 — Nameless Java method recovered as a constructor (`testNamelessInJava`)
- **Change**: `JavaClassOverAst.constructors` required a constructor `METHOD` node to have both no
  return `TYPE` **and** an `IDENTIFIER`, so a malformed nameless declaration like `void () {}`
  (its `void` is an error element, not a return type) was dropped; with no explicit constructor a
  public default constructor was synthesized, so `class K : Nameless()` saw a visible constructor and
  produced no diagnostic. PSI (same syntax parser) treats any no-return-type method as a
  (package-private) constructor, suppressing the default one → `INVISIBLE_REFERENCE`. Dropped the
  `IDENTIFIER` requirement so constructor detection mirrors PSI's `getReturnTypeElement() == null`.
- **Files**: `model/JavaClassOverAst.kt` (constructors filter). Test data unchanged (shared golden).
- **Tests**: full box+phased suite 2839/2839 (0 FAILED); `JavaParsingTest`/`JavaLightTreeTest` green.
- **Result**: regression fixed (new test from master merge; SDK 261 golden).

### 2026-07-14 — Skip inaccessible inherited nested classes (IJ-FP regression: `testIntellij_exceptionAnalyzer`)
- **Change**: `walkSupertypeClassIds` accepted the first inherited nested class of a matching simple
  name regardless of accessibility, so a package-private nested type in a supertype from another
  package (e.g. `SimpleColoredComponent.TextRenderer`) wrongly shadowed a same-named top-level class,
  producing a spurious `RETURN_TYPE_MISMATCH`. Now filtered by JLS 6.6/8.2 accessibility: `private`
  never inherited, package-private only within the declaring package; inaccessible matches expand
  deeper instead of resolving. Visibility read cycle-safely via new `FirJavaClass.nonEnhancedVisibility`
  (from `originalStatus`, no lazy `status`) surfaced through `FirBackedJavaClassAdapter.visibility`.
- **Files**: `resolution/JavaInheritedClassResolver.kt`, `resolution/FirBackedJavaClassAdapter.kt`,
  `fir-jvm/.../FirJavaClass.kt` (+`nonEnhancedVisibility`); test
  `codegen/box/javaDirect/packagePrivateInheritedNestedClassNotVisibleAcrossPackages.kt`.
- **Tests**: box 1179/1179, phased 1513/1513 (0 FAILED); `IntelliJFullPipelineTestsGenerated.testIntellij_exceptionAnalyzer`
  green under java-direct; PSI + CompileKotlinAgainstKotlin gates green (shared `FirJavaClass` edit).
- **Result**: regression fixed.

### 2026-07-13 — Memoize `JavaClassOverAst.supertypes` (IJ-FP regression)
- **Change**: `supertypes` was a recomputing `get()` that returned fresh `JavaClassifierTypeOverAst`
  instances on every read; FIR forces it from two lazy slots per class (`FirJavaClass.superTypeRefs`
  enhancement and `directSupertypeClassIdsCache`), so each supertype's `classifier` (a per-instance
  lazy hitting the symbol provider) resolved from cold twice. Made it `by lazy(PUBLICATION)` so both
  reads share instances and each supertype resolves once. List build allocates wrappers only → still
  resolution-safe.
- **Files**: `model/JavaClassOverAst.kt` (supertypes get()→by lazy).
- **Tests**: box 1178/1178, phased 1513/1513 (0 FAILED).
- **Result**: regression fixed. IntelliJ `testIntellij_platform_ide_impl` warm frontend (isolated bench,
  4 iters, same build): java-direct 21.5s wall / 20.8s CPU vs legacy 25.3s / 23.6s — was ~+8% before.
