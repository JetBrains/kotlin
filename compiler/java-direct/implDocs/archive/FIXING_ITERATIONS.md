# Java-Direct: Fixing Iterations

## Current Status (Single Source of Truth)

| Metric | Value |
|--------|-------|
| **Last Iteration** | 60 (2026-03-27) |
| **Box Tests** | 1168/1168 passing (100%) |
| **Phased Tests** | 1454/1456 passing (99.9%) |
| **Combined** | 2679/2681 passing, **2 failing** (0 remaining, 2 won't fix) |

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

## Completed Features

| Category | Status | Tests Fixed |
|----------|--------|-------------|
| **Sealed Classes** | Done (iter 26) | 9 |
| **Java Records** | Done (iter 27-28) | 8 |
| **Ambiguity Detection** | Done (iter 29-30) | 4 |
| **Raw Types** | Done (iter 33) | 10 |
| **Type Parameter Scoping** | Done (iter 34) | 3 |
| **Enum Handling** | Done (iter 36) | 14 |
| **Implicit Modifiers** | Done (iter 37) | 5 |
| **Import Resolution** | Done (iter 37b, 43, 46) | 16 |
| **TYPE_USE Annotations** | Done (iter 44-45) | 16 |
| **package-info.java** | Done (iter 48) | 1 |
| **Flexible Type Rendering** | Done (iter 49) | 4 |
| **Interface Abstractness / Static Inner Scoping** | Done (iter 50) | 7 |
| **Static-Imported Const Vals in Annotations** | Done (iter 51) | 2 |
| **Multi-Dimensional Array Types** | Done (iter 52) | 4 |
| **Annotation Default Binary Expressions** | Done (iter 53) | 2 |
| **External Class Flexible Type Rendering** | Done (iter 54) | 1 |
| **Wrong-Arity Type Argument Handling** | Done (iter 55) | 2 |
| **Annotation Class Inheritance Detection** | Done (iter 59) | 1 |
| **JSpecify Foreign Annotations** | Done (iter 60) | 2 |

---

## Archived Iterations

| Archive | Iterations | Result |
|---------|------------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1-6 | 0 -> 90/138 (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7-16 | 90 -> 532/601 (88.5%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17-23 | 1075 -> 1134/1166 (97.2%) |
| `implDocs/archive/ITERATIONS_24_26_DETAILS.md` | 24-26 | 1134/1166 -> 1150/1167 (98.5%) |
| `implDocs/archive/ITERATIONS_27_36_DETAILS.md` | 27-36 | 1150/1167 -> 1157/1168 box, **79 combined failing** |
| `implDocs/archive/ITERATIONS_37_51_DETAILS.md` | 37-51 | 1157/1168 -> 1165/1168 box, **17 combined failing** |

**Key patterns established** (see archives and `ITERATION_RESULTS.md`):
- Callback pattern for resolution (types, annotations, enums, constants)
- Two-phase type parameter construction
- PSI/java-direct discrimination in shared FIR code
- Implicit supertypes for Java special class kinds
- java.lang implicit import handling
- Protected static vs protected and package visibility distinction

### All Iterations

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
| 51 | Static-imported Kotlin const vals in Java annotations: IMPORT_STATIC_STATEMENT parsing, staticImportResolution for bare names, FirExpressionEvaluator for const eval | +2 box |
| 52 | Multi-dimensional array types: KMP parser flat brackets fix in createJavaType() | +2 box, +2 phased |
| 53 | Annotation default binary expressions: evaluateConstantExpression handles BINARY_EXPRESSION (int addition, string concat) | +1 box, +1 phased |
| 54 | External class flexible type rendering: isTriviallyFlexibleHint checks import-resolved FQN against read-only collection set | +1 phased |
| 55 | Wrong-arity type argument handling: isRaw for fewer-args, truncation for more-args, ConeRawScopeSubstitutor AIOOBE fix | +2 phased |
| 56 | Transitive inherited inner class resolution; abstract member detection through generic Java hierarchy | +2 phased |
| 57 | Outer type args for inherited inner classes in K-J-K hierarchy | +1 phased |
| 58 | Investigation of javac sealed package failures (Problems 2 & 8) — won't fix | +0 |
| 59 | Annotation class inheritance detection: skip kotlin.* import resolution + reject FIR builtins in tryResolve | +1 phased |
| 60 | JSpecify foreign annotations: missing `withThirdPartyJava8Annotations()` in build.gradle.kts; varargs annotation placement on component type | +2 phased |

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
- Box: X/1168, Phased: X/1456

### Key Learnings
[What to add to AGENT_INSTRUCTIONS.md or implDocs/?]
```
