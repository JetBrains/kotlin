# Java-Direct: Iteration Results Log

**Current status**: `:compiler:java-direct:test` full suite green. No known won't-fix.

**Last archived**: `implDocs/archive/ITERATION_RESULTS_2026_08_26.md` (entries through 2026-08-26).

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
- **Archive when this file passes ~600 lines** (see `AGENT_INSTRUCTIONS_COMMON.md` →
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

### 2026-09-03 — comment rules moved from review time to edit time
- **Change**: the comment gate kept firing only after a change was reported, so the rules were
  restated as an edit-time default. Non-negotiable rule 6 split into 6a (write the edit without the
  comment; add it back only with a named justification) and 6b (the pre-report `git diff` gate); a
  five-line "At edit time" card now opens the Source Comment Conventions; the register-carry-over
  bullet forbids pasting a sentence from an analysis, log entry or user reply into a comment; two
  rows added to the rejected-shapes table (peer/phase-order justification, KDoc on a trivial
  accessor). The module file gained rule 4 pointing at the same gate.
- **Files**: `AGENT_INSTRUCTIONS_COMMON.md` (+19), `AGENT_INSTRUCTIONS.md` (+5).
- **Tests**: not run — docs-only.
- **Result**: green.

### 2026-09-03 — comment pass over the facade-qualifier change
- **Change**: reread the comment lines of the entry below against the comment conventions.
  `tryResolveAsTopLevel`'s KDoc lost its counterfactual half and keeps the testData reference,
  `jvmFacadeClassName`'s KDoc lost the peer/phase-order paragraph and states the contract only,
  and the `nullableFirProvider` KDoc was deleted.
- **Files**: `resolution/JavaExternalConstResolver.kt` (−7 comment lines).
- **Tests**: `JavaUsingAstBoxTestGenerated` green.
- **Result**: green — comments only, no behavior change.

### 2026-09-03 — cross-language const resolution: the facade fallback ignored the Java qualifier
- **Change**: `resolveExternalFieldValue` tried the top-level/facade lookup on the qualifier's
  *package* only, so a Java `Holder.VALUE` resolved to a Kotlin top-level `const val VALUE` of the
  same package — the frontend saw `1` where javac saw `2`. `tryResolveAsTopLevel` now takes the
  qualifier's simple name and keeps only properties whose declaring file compiles into a facade of
  that name (`@JvmName` literal, else file name + `Kt`; `JvmPackagePartSource` when deserialized).
  Same gate applied to the annotation-argument entry point `resolveConstFieldValue`.
- **Files**: `resolution/JavaExternalConstResolver.kt` (+30/−4), 2 new
  `testData/codegen/box/javaDirect/javaConstantQualifiedBy{JavaClassVsKotlinTopLevelConst,KotlinFacade}.kt`.
- **Tests**: box + phased 2818/2818 green; the mismatch fixture verified red without the fix; both
  fixtures also green on the PSI path (`FirLightTreeBlackBoxCodegenTestGenerated$Box$JavaDirect`).
- **Result**: green — reported in the "Non-literal arguments in JvmName" discussion. A non-literal
  `@file:JvmName` argument still resolves here (it is const-evaluated before Java resolution reads
  it), so the facade name it yields remains phase-dependent; that part needs the proposed diagnostic.

### 2026-09-03 — comment pass over the `ClassId` contract change
- **Change**: reread the comment lines of the entry below against the comment conventions. The
  `JavaClassFinder.findClass` KDoc, the `takeIf` note in `KotlinCliJavaFileManagerImpl` and
  `tryCreateJavaClass`'s KDoc lost their narrative and counterfactual halves; the contract is stated once
  on the interface, the two implementations keep a one-line pointer.
- **Files**: `JavaClassFinder.kt`, `KotlinCliJavaFileManagerImpl.kt`, `KotlinJavaPsiFacade.java` (−12 comment lines).
- **Tests**: `CliTestGenerated` diagnostic tests green.
- **Result**: green — comments only, no behavior change.

### 2026-09-03 — `ClassId` contract enforced in the CLI finder; `javaSrcWrongPackage` pinned in both Java views
- **Change**: the previous entry's `javaSrcWrongPackage.out` re-baseline only held because java-direct is
  default-on. `KotlinCliJavaFileManagerImpl.findClass(Request)` now drops a PSI class whose own `ClassId`
  differs from the requested one — it is the only `JavaClassFinder` resolving by file location, so the only
  one which could return `foo.A` for a request for `<root>.A`. The rule is stated on
  `JavaClassFinder.findClass`/`findClasses`, which legitimizes the `CliFinder` branch of
  `KotlinJavaPsiFacade.findClass` and the check KT-62892 removed from `FirJavaFacade.findClass`.
  The KT-11474 `IllegalStateException("Requested … got …")` in `tryCreateJavaClass` is subsumed by its
  `classId` check and was deleted; K1's `LazyJavaPackageScope` guard left as is.
- **Files**: `KotlinCliJavaFileManagerImpl.kt`, `KotlinJavaPsiFacade.java`, `JavaClassFinder.kt`,
  new `testData/cli/jvm/diagnosticTests/javaSrcWrongPackagePsi.{args,out}` (`-Xjava-direct=false`),
  `CliTestGenerated.java`.
- **Tests**: `CliTestGenerated` 41/41 diagnostic tests + all groups green; `:compiler:java-direct:test`
  green; PSI gate `PhasedJvmDiagnosticLightTreeTestGenerated` green. The new PSI fixture verified red
  without the finder check.
- **Result**: green — both Java views now report `unresolved reference 'A'`.

### 2026-09-02 — CLI tests with java-direct default-on: FindJavaClass perf, single-file roots, records
- **Change**: (1) `PerformanceManager` wired CLI → `JavaClassFinderOverAstImpl.findClass`
  (`FindJavaClass` side time). The count was `2` vs PSI's `1` because the implicit `java.lang.Object`
  supertype went through full JLS scope resolution, whose first step probed `Outer.java` as a nested
  class through the symbol provider and back into the finder; `SimpleClassifierType` /
  `EnumSupertypeForJavaDirect` now use `resolveCanonicalName` (existence probe of the canonical
  `ClassId`, no scope walk). (2) Single-file source roots are exempt from the KT-4455 basename gate
  (`FileEntry.isSingleFileRoot`), as in PSI's `SingleJavaFileRootsIndex`; the earlier all-roots
  recursive fallbacks (`findAllFilesForTopLevelClass`, `hasTopLevelClassAnywhere`) were dropped — they
  broke lazy per-package indexing and were never needed (root-level wrong-package files were already
  registered under their declared package). (3) Records get the implicit `java.lang.Record` supertype
  (JLS 8.10) — `recordAsSingleFileRoot` now reports `MISSING_DEPENDENCY_SUPERCLASS` like PSI.
  (4) `javaSrcWrongPackage.out` re-baselined to java-direct's `unresolved reference 'A'`: this is the
  behaviour the test was added for (KT-11474) and what the IDE reports in both K1 and K2 — KT-62892's
  fix (37ccf5d4f363) checks `classId` in `KotlinJavaPsiFacade.tryCreateJavaClass`, but the
  `CliFinder` branch bypasses that check and the `FirJavaFacade.findClass` `takeIf { classId == … }`
  was removed, so only the PSI *CLI* resolves `<root>.A` from a file declaring `package foo`.
- **Files**: `JavaClassFinderOverAstImpl.kt`, `JavaPackageIndexer.kt`, `model/JavaClassOverAst.kt`,
  `model/JavaTypeOverAst.kt`, `resolution/JavaTypeResolver.kt`, `JavaDirectJavaInterop.kt`,
  `JavaInterop.kt`, `JvmFrontendPipelinePhase.kt`, `JavaParsingClassFinderTest.kt` (+3 tests),
  new `JavaImplicitSupertypeResolutionTest.kt` (canonical probe only, no shadowing by `Outer.java`;
  `StubSymbolProvider` moved to `JavaParsingTestBase.kt`), `testData/cli/jvm/diagnosticTests/javaSrcWrongPackage.out`.
- **Tests**: `:compiler:java-direct:test` green; CLI `reportPerf*`, `singleJavaFileRoots`,
  `recordAsSingleFileRoot`, `javaSrcWrongPackage`, `fullyQualifiedDeepJava*` green.
- **Result**: green.

### 2026-09-02 — Valhalla value/record flags: `isValue` via MODIFIER_LIST, implicit finality
- **Change**: TeamCity `TestsWithValhalla` failures with java-direct default-on. `isValue` used `findChildByType` on CLASS (missed `value` inside MODIFIER_LIST) → switched to `hasModifier(VALUE_KEYWORD)`. `isFinal` now treats records and value classes as implicitly final (JLS). Fixes INNERCLASS flags and `LoadableDescriptors` for Java value classes.
- **Files**: `model/JavaClassOverAst.kt`, `JavaParsingClassFinderTest.kt` (value/record assertions on `V`/`NV`).
- **Tests**: `:compiler:java-direct:test` full suite green (2839). Valhalla blackbox tests skip locally without Valhalla JDK; TC has it.
- **Result**: green — two root causes in AST flag mapping.

### 2026-08-27 — comment self-check promoted to a non-negotiable rule
- **Change**: three of the four instruction fixes proposed after the repeated comment passes.
  The self-check moved from the closing paragraph of the comment section into the
  `⚠ Non-Negotiable Rules` block as rule 6, with the `git diff -U0 | grep` command that found the
  violations both times; a "Rejected comments and their replacements" table added next to the gate
  (narrative / counterfactual / restatement / fact in two places, real lines from this module); and a
  rule against carrying the register of an analysis or review reply into code comments. The
  root-`AGENTS.md` pointer (proposal C) was left out on request.
- **Files**: `AGENT_INSTRUCTIONS_COMMON.md` (+24 lines).
- **Tests**: not run — docs-only.
- **Result**: green.

### 2026-08-27 — third comment pass: comments restating the code deleted
- **Change**: review find — several comments narrated the lines below them. Deleted the static-outer chain
  note (the loop's own condition), the prose list of the three single-name lookup calls (the javac
  divergence and the two pinning testData files kept), the `finderOver` KDoc, and the recovery half of the
  out-of-scope note; the type-parameter identity trap moved to the return that hands the declaring class's
  own instances over, and `firBackedJavaType`'s KDoc reduced to what `declarationChainRoot` is.
- **Files**: `model/JavaTypeOverAst.kt`, `JavaParsingImplicitOuterTypeArgumentsTest.kt` (−13 comment lines).
- **Tests**: `:compiler:java-direct:test` green.
- **Result**: green — comments only, no behavior change.

### 2026-08-27 — comment pass over `JavaClassifierTypeOverAst.computeClassifier`
- **Change**: reread the pre-existing comments in the body of `computeClassifier` against the comment
  conventions. The numbered walkthrough of the single-name lookup order collapsed to the javac divergence
  and the two testData files that pin it; the justification of the in-scope pass shortened to why it runs
  before the `resolve` fallback; the cross-file comment deleted (`classifierAdapterFor` documents it) and
  the duplicated KT-87797 TODO dropped (it lives on `findInheritedTypeParameter`).
- **Files**: `model/JavaTypeOverAst.kt` (−14 comment lines).
- **Tests**: box + phased + module unit tests green.
- **Result**: green — comments only, no behavior change.

### 2026-08-27 — comment pass over the raw-type change
- **Change**: reread the comment lines of the previous entry's diff against the comment conventions.
  Deleted the KDoc of `unknownArguments` and three narrative block comments in the tests, dropped the
  counterfactual passages (rejected fallback, "instead of degrading to `List<*>`") and the coverage
  meta-commentary in the incremental test, moved the type-parameter identity trap to a single place
  (`firBackedJavaType`), and shortened the two new testData headers.
- **Files**: `model/JavaTypeOverAst.kt`, `resolution/JavaTypeResolver.kt`,
  `JavaParsingImplicitOuterTypeArgumentsTest.kt`, `JavaParsingTypeSystemTest.kt`,
  `IncrementalJavaClassFromPreviousOutputTest.kt`, 2 `testData/diagnostics/tests/j+k/*.kt` (−45 comment lines).
- **Tests**: box + phased + module unit tests green; PSI and light-tree phased gates green.
- **Result**: green — comments only, no behavior change.

### 2026-08-27 — raw-ness derived from `null` type arguments; `firBackedJavaType` gets a declaration-chain root
- **Change**: second review round on PR #7500. `JavaClassifierTypeOverAst.typeArguments` now emits one entry
  per type parameter it has to supply and `null` where nothing is known (PSI's contract and order), and
  `isRaw` is read off it — `computeIsRaw` and `isQualifiedByInheritor` deleted. Fixes the case where a
  simple-name reference to an inner class of a generic outer, from a class that neither encloses nor inherits
  it, produced `ConeErrorType(ConeUnresolvedNameError)` instead of a raw type. `firBackedJavaType` takes a
  `declarationChainRoot` and owns the flexible unwrap plus an explicit `ConeTypeParameterType` arm, so nested
  recovered arguments no longer degrade to `*`; `recoveredOuterTypeArgument` collapsed into one call.
  `IncrementalJavaClassFromPreviousOutputTest` split into the attribute-present and attribute-stripped pair.
- **Files**: `model/JavaTypeOverAst.kt`, `resolution/JavaTypeResolver.kt`,
  `JavaParsingImplicitOuterTypeArgumentsTest.kt`, `JavaParsingTypeSystemTest.kt`,
  `IncrementalJavaClassFromPreviousOutputTest.kt`, 2 new `testData/diagnostics/tests/j+k/*.kt`.
- **Tests**: box + phased green; module unit tests 154/154; PSI and light-tree phased gates green;
  both new testData files verified red without the fix.
- **Result**: green — rationale in `implDocs/RAW_TYPE_ARGUMENT_UNIFICATION_2026_08_27.md`.

### 2026-08-26 — docs pass: log archived, instructions split into common + module parts
- **Change**: archived the iteration log and the fully-landed `MERGED_REFACTORING_PLAN_2026_05_04.md`;
  split `AGENT_INSTRUCTIONS.md` into a module-independent `AGENT_INSTRUCTIONS_COMMON.md`
  (shell/Gradle discipline, comment and writing style, simplification discipline, docs maintenance)
  and the java-direct-specific remainder; rewrote the comment/writing rules from a review of the
  branch's session logs and a comparison against human-written compiler modules; trimmed `ReadMe.md`
  (scenarios now live only in `implDocs/RESOLUTION_SCHEMA.md`).
- **Files**: `AGENT_INSTRUCTIONS.md`, new `AGENT_INSTRUCTIONS_COMMON.md`, `ReadMe.md`,
  `ITERATION_RESULTS.md`, `implDocs/archive/*` (2 moves + banners).
- **Tests**: not run — docs-only.
- **Result**: green.
