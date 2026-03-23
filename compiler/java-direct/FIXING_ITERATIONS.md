# Java-Direct: Fixing Iterations

## Current Status (Single Source of Truth)

| Metric | Value |
|--------|-------|
| **Last Iteration** | 51 (2026-03-23) |
| **Box Tests** | 1164/1168 passing (99.6%) |
| **Phased Tests** | 1425/1443 passing (98.7%) |
| **Combined** | ~2589/2611 passing, **19 failing** |

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration.

---

## Estimation Rules

Estimates have been consistently wrong (5-60% accuracy). Follow these rules:

1. **Debug 2-3 representative tests** before estimating — run with exception debugging, trace actual code path
2. **Categorize by code path**, not test name or error message — different symptoms often have different root causes
3. **Apply 50% discount** for unfamiliar code areas
4. **Maximum estimate**: 15 tests per category unless proven otherwise
5. **Do NOT trust code snippets** in this document — always verify AST node names via debugging first

---

## Remaining Failures (36 tests)

### Completed Features (iterations 26-29)

| Category | Status                  | Tests Fixed           |
|----------|-------------------------|-----------------------|
| **Sealed Classes** | Done (iter 26)          | 9 tests fixed         |
| **Java Records** | Done (iter 27, iter 28) | all record tests pass |
| **Ambiguity Detection** | Done (iter 29-30)       | 4 tests fixed (including cross-file) |
| **Raw Types** | Done (iter 33)          | 10 tests fixed        |

### Next Priorities

#### Ambiguity Detection — ✅ DONE (iter 29-30)

**Status**: All 4 tests fixed, including cross-file detection.

**Tests fixed**:
- ✅ `testInheritanceAmbiguity` (same-file)
- ✅ `testInheritanceAmbiguity2` (cross-file)
- ✅ `testInheritanceAmbiguity3` (same-file)
- ✅ `testInheritanceAmbiguity4` (cross-file)

#### Raw Types — ✅ DONE (iter 33)

**Status**: 10 tests fixed. Two edge cases remain.

**Root causes fixed**:
1. Java Model `isRaw` — empty `REFERENCE_PARAMETER_LIST` was treated as having type args
2. FIR raw type detection — star imports returned simple names, not FQN

**Remaining failures (2)**:
- `testPseudoRawTypes` — Java compilation error (custom `java.util.Collection`)
- `testRawSupertypeOverride` — Complex raw supertype inheritance

#### Type Parameter Scoping — ✅ DONE (iter 34)

**Status**: 3 tests fixed.

#### Import/Package Edge Cases — ~8-10 tests — LOW CONFIDENCE

**Problem**: Complex scenarios where package names clash with class names, or nested imports clash with top-level classes.

**Tests to debug first**: `testTopLevelClassVsPackage`, `testNestedClassClash`, `testCurrentPackageAndExplicitNestedImport`

**Approach**: Review import resolution in `JavaResolutionContext.kt`.

#### Records FIR Integration — **DONE** (iter 28)

All 6 record tests pass. See iteration 28 in `ITERATION_RESULTS.md` for details.

#### Enum Handling — ~3-5 tests — LOW CONFIDENCE

**Tests**: `testEnumEntriesFromJava`, `testStaticImportFromEnumJava`, `testJavaEnum`

**Status**: Needs investigation of enum entries generation for java-direct.

#### Baseline Differences — ~50-60 tests — NEEDS TRIAGE

**IMPORTANT**: Many "baseline diffs" are real java-direct bugs, not cosmetic differences.

**Triage process** (for each test):
1. Run with PSI-based FIR: `./gradlew :compiler:fir:fir-jvm:test --tests "FirLightTreeBlackBoxCodegenTestGenerated.*testName*" -q`
2. If PSI passes but java-direct fails -> real java-direct bug, recategorize by actual error
3. If both fail -> general FIR issue, exclude from java-direct scope

**Known real bugs in this category** (from iteration 25c investigation):
- `testInheritedInnerAndNested` was a real `isStatic` bug, not a cosmetic diff

---

## Archived Iterations

| Archive | Iterations | Result |
|---------|------------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1-6 | 0 -> 90/138 (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7-16 | 90 -> 532/601 (88.5%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17-23 | 1075 -> 1134/1166 (97.2%) |
| `implDocs/archive/ITERATIONS_24_26_DETAILS.md` | 24-26 | 1134/1166 -> 1150/1167 (98.5%) |

**Key patterns established** (see archives and `ITERATION_RESULTS.md`):
- Callback pattern for resolution (types, annotations, enums, constants)
- Two-phase type parameter construction
- PSI/java-direct discrimination in shared FIR code
- Implicit supertypes for Java special class kinds
- java.lang implicit import handling
- Protected static vs protected and package visibility distinction

### Recent Iterations (24-27)

| Iteration | Category | Tests Fixed |
|-----------|----------|-------------|
| 24 | Constant evaluation, protected static, sibling inner classes | +15 |
| 24b | Cyclic type bounds StackOverflowError | +6 |
| 25 | Inherited inner class resolution | +2 |
| 25c | Interface nested class static flag | +8 |
| 26 | Sealed classes (`isSealed`, `permittedTypes`) | +9 |
| 27 | Java records (Java Model only, FIR integration pending) | +0 |
| 28 | Java records FIR integration (isRecord token fix, isVararg fix, canonical ctor detection) | +6 phased, +2 box |
| 29 | Ambiguity detection for inner classes (same-file only) | +2 phased |
| 30 | Cross-file ambiguity detection | +2 phased |
| 31 | JavaParsingTest regressions fix | +0 (regression fix) |
| 32 | Kotlin constants in Java annotations | +2 phased |
| 33 | Raw types detection fix | +8 phased, +2 box |
| 34 | Type parameter identity across class finder lookups | +3 phased |
| 35 | Unresolvable enum annotation argument crash fix | +1 phased |
| 36 | Java enum entries, enum constant visibility, nested class visibility | +2 box, +12 phased |
| 37 | isFinal/isAbstract for enums, findAnnotation on classes, isNative, constructor isFinal, isDeprecatedInJavaDoc | +3 box, +2 phased |
| 37b | Explicit import priority, duplicate star import, findClassId for resolved nested types (removed default-pkg lookup to fix regressions) | +0 box, +7 phased |
| 38 | `hasConstantNotNullInitializer` correctness (method calls/unresolvable refs → false); malformed constructor filter (IDENTIFIER required) | +0 box, +4 phased |
| 39 | Sealed class inheritors (cross-file permits fallback + implicit permits detection); InheritedInner2 package-prefix for dotted supertypes | +0 box, +3 phased |
| 40 | isObjectMethodInInterface for unresolved Object; rawTypeName strips generics through dots; typeArguments uses collectAllRefParamLists + resolves outer type params | +1 box, +2 phased |
| 41 | Reference-first audit: JavaValueParameterOverAst.type passes modifier list annotations; JavaTypeParameterOverAst.upperBounds handles TYPE children; annotations reads MODIFIER_LIST | +0 (full suite count same, individual annotation tests pass) |
| 42 | Fix rawTypeName to exclude annotations from type names (extract identifiers from AST, not text); remove unused JAVA_LANG_TYPES | +1 phased |
| 43 | ClassId-based resolution (`resolveToClassId`) to fix package vs nested class ambiguity (JLS 6.5.2) | +1 phased |
| 44 | TYPE_USE annotation filtering fix: type-position annotations returned unconditionally, member annotations callback-filtered | +13 phased |
| 45 | Type parameter direct annotations: KMP parser places annotations as direct children, not in MODIFIER_LIST | +3 phased |
| 46 | Import resolution: nested class FQN splits, class-level star imports, first-import-wins, cross-file ambiguity with JLS 8.5 shadowing | +4 phased |
| 47 | KT-4455: non-canonical Java classes (class F in E.java where no class E exists) not indexed; knownClassNamesInPackage returns only canonical names | +2 phased |
| 48 | package-info.java support: annotations in PACKAGE_STATEMENT→MODIFIER_LIST→ANNOTATION; JavaPackageOverAst.annotations now populated | +1 phased |
| 49 | isTriviallyFlexibleHint: cross-file Java source classes now produce isTrivial=true in ConeFlexibleType via index-only lookup; fixes ft<T,T?> vs T! FIR dump mismatch | +4 phased |
| 50 | Fix isAbstract for interface methods (DEFAULT_KEYWORD in MODIFIER_LIST, not direct child); fix inherited type param scope for static inner types; fix local class/type param resolution order | +7 |
| 51 | Investigation only: IMPORT_STATIC_STATEMENT parsing (KMP uses separate node type for static imports); AnnotationCodegen defensive skip for IrErrorExpression; no test count change | +0 |

---

## Recommended Approach

See `AGENT_INSTRUCTIONS.md` — Triage, Fixing Approach, Iteration Process, and Revert-First Policy sections.

---

## Future Iteration Template

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Status
- [ ] In Progress / Completed

### Root Cause Analysis
[Debug 2-3 representative tests first. Document actual cause.]

### Fix
[Solution description. Files modified.]

### Test Results
- Box: X/1167, Phased: X/1442

### Key Learnings
[What to add to AGENT_INSTRUCTIONS.md or implDocs/?]
```

---

## Document Change Log

- 2026-03-16: Iteration 35 (enum annotation crash fix) complete, updated metrics to 93 failures
- 2026-03-16: Iteration 34 (type parameter identity) complete, updated metrics to 94 failures
- 2026-03-16: Iteration 33 (raw types) complete, updated metrics to 98 failures
- 2026-03-13: Restructured — merged TEST_FAILURE_ANALYSIS content, removed code snippets, updated metrics to post-iter-27
- 2026-03-12: Iteration 24 complete, updated remaining work analysis
- 2026-03-12: Consolidated iterations 17-23 to archive
