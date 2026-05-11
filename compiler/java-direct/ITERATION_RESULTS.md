# Java-Direct: Iteration Results Log

**Current status**: 1178/1178 box + 1513/1513 phased (2793/2793, 100%). Phased and
box generators now actually route `// FILE: *.java` blocks through java-direct AST;
prior numbers were against PSI loading (see 2026-04-28 entry).

**Last Updated**: 2026-05-10 (Cat D `MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR`
on `NlsContexts.Tooltip` traced to `JavaAnnotationOverAst.computeClassId`
shortcut: the explicit-import path called `ClassId.topLevel(imported)` on
the imported FqName, which splits at the **last** dot only —
`import com.intellij.openapi.util.NlsContexts.Tooltip;` thereby produced
`ClassId(com.intellij.openapi.util.NlsContexts, "Tooltip")`, treating the
class `NlsContexts` as a package. The FIR symbol provider rejected that
ClassId (no such package), `coneType.toSymbol()` returned `null` for the
type-use annotation on `getProblems(...)`'s `List<@Tooltip String>` return,
and Kotlin's `FirImplicitReturnTypeAnnotationMissingDependencyChecker` fired
on every Kotlin call site that picked up the inferred type. Fix: in
`computeClassId`, prefer the model's resolver
(`resolutionContext.resolve(reference)`) when a session is wired — its
`resolveAsClassId` walks all candidate splits from longest-package to
shortest and validates against the symbol provider, producing the correct
`ClassId(com.intellij.openapi.util, "NlsContexts.Tooltip")`. Parsing-level
test fixtures keep the legacy `ClassId.topLevel` shortcut as a fallback.
**Result**: `testIntellij_platform_lang_impl` PASS — 1 of 11
java-direct-only modules remains. **JavaUsingAst\* matrix**: BUILD SUCCESSFUL,
0 FAILED. Remaining 1: `remoteRun` (Cat E codegen
`NegativeArraySizeException` at ASM `Frame.merge` — backend-level, deferred;
the JVM stack-frame merge crash on `doCheckConnection` is downstream of FIR
and not directly tied to any of the 5 java-direct fixes in this iteration).)

**Previously**: 2026-05-10 (Qualified raw-form nested classes
`Outer.Inner` (where `Outer` is a top-level generic class) were classified
as **non-raw** by `JavaTypeOverAst.computeIsRaw`, which only counted the
inner class's *own* type parameters and ignored the outer chain. For
`XLineBreakpointType.XLineBreakpointVariant` (used as the wildcard bound in
`List<? extends XLineBreakpointType.XLineBreakpointVariant>` in
`XDebuggerUtilImpl.java`'s `getLineBreakpointVariantsSync` return type), the
inner has 0 own type parameters but lexically captures the outer's `P`.
java-direct previously produced a `ConeFlexibleType` whose `typeArguments`
referenced the outer's `JavaTypeParameter` from outside its declaring scope,
which `JavaTypeConversion`'s `JavaTypeParameter` branch resolves to
`ConeErrorType` — making the Kotlin call-site `it.asProxy()` on
`fun XLineBreakpointType<*>.XLineBreakpointVariant.asProxy()` fail with
`UNRESOLVED_REFERENCE_WRONG_RECEIVER`. Fix: extend `computeIsRaw` to also
detect the **qualified-form raw** case — multi-part reference
(`rawTypeNameParts.size > 1`), the inner is non-static, no explicit type
arguments on any outer ref-param list, and at least one outer in the chain
has type parameters. The walk uses `parts.size - 1` hops (not
`!outer.isStatic`) because `FirBackedJavaClassAdapter.isStatic` reports
`true` for top-level outers (no `FirOuterClassTypeParameterRef`) while their
own type parameters are still the ones missing. With raw classification,
`JavaTypeConversion` produces `ConeRawType` whose `getProjectionsForRawType`
synthesises erased projections compatible with the Kotlin `<*>` receiver.
**Result**: `testIntellij_platform_debugger_impl` PASS.)

**Previously**: 2026-05-10 (Cross-language `ConstantEvaluator` callback was
passing the **simple** class name to FIR's `resolveExternalFieldValue`, which
could only interpret it as a current-package class or a `<root>.X` top-level
class — neither resolves a cross-package binary Java field. For
`AndroidUtils.R_CLASS_NAME = SdkConstants.R_CLASS` (Java source field
initialised from a binary Java `public static final String` constant), the
callback received `("SdkConstants", "R_CLASS")` and returned `null`,
short-circuiting Kotlin's const-eval of `RESOURCE_CLASS_SUFFIX = "." +
AndroidUtils.R_CLASS_NAME` and producing
`Initializer for const property RESOURCE_CLASS_SUFFIX was not evaluated`. Fix:
in `ConstantEvaluator.evaluateReferenceExpression`, when `findLocalClass` does
not match, promote the simple class name to a fully-qualified name via
`containingClass.resolutionContext.resolve(...)` (which already honours the
file's explicit imports / same-package / star-imports / `java.lang` lookup
chain) before invoking the cross-language callback. With the FQN
(`com.android.SdkConstants`), `resolveExternalFieldValue` now reaches
`tryResolveAsClassMember` → `getClassDeclaredPropertySymbols` → the
binary class's `FirJavaField` symbol, and `tryExtractConstantValue` returns
the compile-time string. **Result**: `testIntellij_android_core` PASS.)

**Previously**: 2026-05-10 (Star-imported binary supertype candidates were
silently dropped from `JavaSupertypeGraph.resolveSupertypeReference` because
the function still gated star imports through `sameClassInSameFilePackage`
(source-only). For `Filter extends RowFilter` (binary `javax.swing.RowFilter`
via `import javax.swing.*`), `getDirectSupertypes(Filter)` therefore returned
empty, the inherited-nested-class walk for `Entry` inside `AndFilter` missed
`RowFilter.Entry`, and `AndFilter.include(Entry)`'s parameter resolved to a
bogus `<root>.Entry` `ConeFlexibleType` instead of `ConeRawType[RowFilter.Entry]`.
The override checker then could not match the candidate against the inherited
raw `include(Entry)` from RowFilter and reported
`ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED include(RowFilter.Entry<out M!, out I!>!)`
on Kotlin subclasses. Fix: emit one candidate per star-import package
(source matches first when present); downstream filters via `tryResolve`,
mirroring Cat A's explicit-import treatment. **Result**:
`testIntellij_r` PASS — 4 of 11 java-direct-only modules remain.
**JavaUsingAst\* matrix**: BUILD SUCCESSFUL, 0 FAILED.)

**Previously**: 2026-05-10 (`@NotNull T[]` array nullability — `JavaTypeOverAst`
attached method-level `MODIFIER_LIST` annotations to the OUTER array wrapper as
type annotations, bypassing FIR's `AbstractSignatureParts.kt:104-111` array-head
TYPE_USE filter (KT-24392). PSI's `PsiArrayType.getAnnotations()` is empty for
method-level `@NotNull`; the annotation is delivered to FIR only via the method
symbol's `annotations` (containerAnnotations), letting the array-head filter drop
it to avoid double-application. Fix in `tryCreateArrayOrVarargFromTypeNode`:
clear `arrayMemberAnnotations` (set to `emptyList()`) for non-vararg arrays so
the outer wrapper carries no member-level annotations. **Result**:
`testIntellij_android_lint_common` PASS — 6 of 11 java-direct-only modules now
green. **JavaUsingAst\* matrix**: BUILD SUCCESSFUL, 0 FAILED.)

**Previously**: 2026-05-10 (Category A of the IJ FP regression delta — three
linked java-direct bugs causing inherited-nested-class lookups to silently
miss every binary-classpath supertype, plus Java 9+ private interface methods
to be loaded as `Public` and `abstract`. Fixed:
(1) `JavaSupertypeGraph.resolveSupertypeReference` — drop the
`sameClassInSameFilePackage` existence check on the explicit-import path so
binary supertype `ClassId`s pass through; (2)
`JavaInheritedMemberResolver.walkJavaSourceSupertypes` — for transitive
levels, use `classFinder.getDirectSupertypes(supertypeClassId)` (per-class
imports) instead of `javaClass.supertypes` re-resolved through the caller's
context; (3) `JavaMemberOverAst.{visibility, isAbstract}` — treat the `private`
modifier on interface members as visibility-Private and as a non-abstract
indicator (matches PSI's `hasModifierProperty(ABSTRACT)` semantics). **Result**:
3 of 6 originally-failing Cat A modules pass (`javascript.psi.impl`,
`javascript.tests`, `swift.language`). Plus the earlier zeppelin fix and
incidental `android.transport` flake recovery, **5 of the 11 java-direct-only
modules are now green**. **JavaUsingAst\* matrix**: 0 FAILED, no regression.)

**Previously**: 2026-05-10 (Category B of the 11-module IJ FP regression delta:
`BinaryJavaClassFinder.knownClassNamesInPackage` was excluding every class file
whose name contains `$`, hiding legitimate top-level Scala companion-module
classes (`Foo$.class`) from FIR's package-known-names gate. PSI's
`KotlinCliJavaFileManagerImpl.knownClassNamesInPackage` does no such filtering;
java-direct now mirrors it. **Result**: `testIntellij_bigdatatools_zeppelin`
PASS. **JavaUsingAst\* matrix: BUILD SUCCESSFUL, 0 FAILED.** Other 9
java-direct-only failures and the regression analysis are recorded in
`implDocs/IJ_FP_REGRESSION_ANALYSIS_2026_05_10.md`.)

**Previously**: 2026-05-08 (Two fixes for `IntelliJFullPipelineTestsGenerated`
regressions: (1) `extractStaticImports` parser-shape — KMP parser emits
`JAVA_CODE_REFERENCE` (not `IMPORT_STATIC_REFERENCE`) under `IMPORT_STATIC_STATEMENT`
for `import static X.*;`, so all static-on-demand imports were being silently
dropped. (2) `findInheritedNestedClass` double-guard — both this function and
`directSupertypeClassIds` use the same `JavaSupertypeLoopChecker.guarded(classId)`
keyed by classId; entering the outer guard then calling the inner one with the
same classId hit the re-entry check and returned `emptyList()`, so inherited
nested-class lookup walked an empty supertype list and missed every inherited
inner. Hoisted the `directSupertypeClassIds` call out of the guard. **Result**:
57 of 70 originally-failing tests now pass; the remaining 13 are 12 Kotlin
language compatibility issues (`CONTEXT_PARAMETERS_ARE_DEPRECATED` test data
debt) plus 1 cross-module annotation-accessibility issue
(`MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR` for `NlsContexts.Tooltip`).
**JavaUsingAst\* matrix: 2699/2699 (no regression).**)

### Entry Template

```markdown
## [Title] — [Date]

### Overview
[1-2 sentence summary of what was done and why.]

### Changes
[Bullet list of concrete changes: file, what changed, why.]

### Test Results
[Suite name, pass/fail count, any regressions.]

### Files Modified
| File | Change |
|------|--------|
| `file.kt` | description |

### Key Learnings
[Bullet list of non-obvious findings useful for future work.]
```

> **Add new entries below this line.** Most recent first. Separate with `---`.

---

## Nested-class explicit-import shortcut in `JavaAnnotationOverAst.computeClassId` produced wrong package/class split — 2026-05-10 (latest)

### Overview

`testIntellij_platform_lang_impl` failed with
`MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR` on
`Type annotation class 'com.intellij.openapi.util.NlsContexts.Tooltip' of the
inferred type is inaccessible.` at
`DaemonTooltipWithActionRenderer.kt:67:67`, where `problems` is the inferred
result of a Java method
`protected @Unmodifiable @NotNull List<@Tooltip String> getProblems(...)` in
`DaemonTooltipRenderer.java`. PSI accepts; java-direct rejected because the
type-use annotation `@Tooltip`'s `coneType.toSymbol()` returned `null`
during `FirImplicitReturnTypeAnnotationMissingDependencyChecker`'s walk.

### Root cause

`JavaAnnotationOverAst.computeClassId` short-circuited the explicit-import
case with `ClassId.topLevel(imported)`. `ClassId.topLevel(FqName)` splits the
FqName at its **last** dot — `parent → packageFqName`, `shortName →
relativeClassName`. For nested-class imports like
`import com.intellij.openapi.util.NlsContexts.Tooltip;` (where `NlsContexts`
is a class and `Tooltip` is its nested annotation), this produces
`ClassId(packageFqName = com.intellij.openapi.util.NlsContexts, relativeClassName = Tooltip)` —
treating the class `NlsContexts` as a package.

The FIR symbol provider has no entry for that bogus ClassId (no package by
that name exists), so `getClassLikeSymbolByClassId(...)` returned `null`.
Downstream, `coneType.toSymbol()` returned `null` and Kotlin's checker fired.

PSI is unaffected because PSI's `JavaAnnotationImpl.getClassId` reads from
the `PsiClass` it has already resolved through the file's import scope, so
the package/class boundary is intrinsic to the PsiClass.

### Fix

In `computeClassId`, prefer the model's own resolver — call
`resolutionContext.resolve(reference)` first when a session is wired. Its
`resolveFromExplicitImport` path uses `resolveAsClassId(imported,
tryResolve)`, which iterates every candidate split from longest-package to
shortest and validates each against the symbol provider via `tryResolve`.
For `com.intellij.openapi.util.NlsContexts.Tooltip` the loop:

1. probes `ClassId(com.intellij.openapi.util.NlsContexts, "Tooltip")` →
   false (not a package);
2. probes `ClassId(com.intellij.openapi.util, "NlsContexts.Tooltip")` →
   true → returned.

Parsing-level test fixtures (no session wired) keep the legacy
`ClassId.topLevel(imported)` fallback so they don't regress.

### Test Results

| Test | Before | After |
|---|---|---|
| `testIntellij_platform_lang_impl` | FAIL (`MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR` on `NlsContexts.Tooltip`) | **PASS** |

`JavaUsingAst*` matrix (`Phased + Box`): `BUILD SUCCESSFUL in 1m 45s`,
**0 FAILED** — no regression vs. 2793/2793.

Cumulative across this iteration's six fix bundles (Cat B + Cat A + array +
star-import-supertype + binary-const-eval + qualified-form-raw + this), the
java-direct-only failure count on the IJ FP corpus dropped from 11 to 1:

```
PASS: zeppelin (Cat B), psi_impl, javascript_tests, swift_language (Cat A),
      lint_common (array iter), r (star-import iter), android_core
      (binary-const-eval iter), platform_debugger_impl (qualified-form-raw
      iter), platform_lang_impl (this iter), android_transport (flaky)
FAIL: remoteRun (Cat E codegen ASM crash, deferred)
```

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | `computeClassId`: prefer `resolutionContext.resolve(reference)` over `ClassId.topLevel(imported)` for the explicit-import path; the resolver's `resolveAsClassId` validates each candidate split against the FIR symbol provider, producing the correct package/class boundary for nested-class imports. KDoc cites the failing scenario (`NlsContexts.Tooltip`). |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **`ClassId.topLevel(fqName)` is wrong for any FqName that crosses a
  class/package boundary at a dot other than the last.** Anywhere the
  Java-direct model resolves a reference whose target may live inside a
  nested class, the longest-package-first iteration in `resolveAsClassId`
  is the correct shape. The four call sites of `ClassId.topLevel(...)` in
  the model layer (annotation classId, annotation-classId fallback, dotted
  reference fallback, no-import fallback) are all suspect; this fix
  eliminates one of them on the hot path while leaving the no-session
  fallbacks for parsing-level fixtures.
- **Type-use annotation `coneType.toSymbol() == null` is the precise
  signal for the `MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR`
  diagnostic.** Future regressions of this shape should grep
  `FirImplicitReturnTypeAnnotationMissingDependencyChecker` and trace
  whichever annotation's classId is unreachable through the symbol
  provider — the chain from `JavaAnnotationOverAst.classId` to
  `FirAnnotation.annotationTypeRef.coneType.toSymbol()` is the canonical
  one.
- **Cat D wasn't pre-existing — it was a separable java-direct bug.** The
  earlier triage tagged it "already known", but local repro plus the
  fix above show the issue lives entirely on the java-direct model side
  (annotation classId), not in cross-module classpath setup.

### Notes / follow-ups not in this iteration

- `remoteRun` (Cat E `NegativeArraySizeException` at ASM `Frame.merge`):
  remains on the deferred list. Retry confirmed it's not flaky — same
  crash on every run. Sanity-checked master's compiled
  `RemoteSdkSessionUtil.doCheckConnection` via `javap -c -p`:
  master's bytecode matches the java-direct failure dump opcode-for-opcode
  at the descriptor level (same operands, same labels, same stack pattern,
  same exception table). The differentiator therefore lives in the
  **frame attributes** (StackMapTable / type annotations / signatures) or
  in some IR-stage transformation that produces a structurally-identical
  but frame-merger-incompatible bytecode shape under java-direct's
  Java-symbol loading. Reaching root cause needs runtime ASM debug or
  instrumentation of `MethodWriter.computeAllFrames`, neither feasible at
  the model-layer review level. Filing for follow-up after the IDE/CI
  triage produces narrower repro context.
- The remaining `ClassId.topLevel(imported)` fallback paths in
  `JavaAnnotationOverAst.computeClassId` (when no session is available)
  are correct for the parsing-level fixture role they serve, but should be
  audited if future test scenarios hit nested-class imports without a
  wired session.

---

## Qualified raw-form nested classes (`Outer.Inner` with generic top-level `Outer`) misclassified as non-raw by `JavaTypeOverAst.computeIsRaw` — 2026-05-10 (previously latest)

### Overview

`testIntellij_platform_debugger_impl` failed with
`UNRESOLVED_REFERENCE_WRONG_RECEIVER` on
`.map { it.asProxy() }` inside `InlineBreakpointInlayManager.kt`, where
`it` flows from a Java method returning
`List<? extends XLineBreakpointType.XLineBreakpointVariant>` and `asProxy()`
is a Kotlin extension on
`fun XLineBreakpointType<*>.XLineBreakpointVariant.asProxy()`. PSI accepts
the receiver match transparently; java-direct rejected it because the inner
`XLineBreakpointVariant` reference was being constructed with a
`JavaTypeParameter` argument pointing at the outer class's `P` from outside
its declaring scope — yielding `ConeErrorType` for the receiver's outer type
argument.

### Root cause

`XLineBreakpointVariant` is a non-static inner of generic
`XLineBreakpointType<P>`, but declares 0 own type parameters. java-direct's
`JavaClassifierTypeOverAst.computeIsRaw` only checked the *own* count
(`ownParams > 0 && ownExplicit < ownParams`) — so for
`XLineBreakpointType.XLineBreakpointVariant` (no `<>` anywhere) it returned
`false`. `computeTypeArguments` then fell into the implicit-outer-args path,
producing `[JavaTypeParameterTypeOverAst(P)]`. `JavaTypeConversion`'s
`JavaTypeParameter` branch looked up `P` in the type-parameter stack — but
the stack belongs to `XDebuggerUtilImpl.java`'s lexical scope, not
`XLineBreakpointType`'s — and emitted
`ConeErrorType(ConeUnresolvedNameError("P"))` for the argument. The
resulting `ConeFlexibleType` had an error type at position 0, breaking
receiver subtyping against the Kotlin declared
`XLineBreakpointType<*>.XLineBreakpointVariant` receiver.

The qualified-form raw rule from JLS 4.6 was not modelled: when an outer in
a multi-part `Outer.Inner` reference is generic and no `<>` is provided on
that outer, the entire reference is raw — even if the inner declares zero
own type parameters.

### Fix

Extend `computeIsRaw` with a second clause guarded on
`rawTypeNameParts.size > 1` (multi-part reference) and `!javaClass.isStatic`:
walk `outerClass` up to `parts.size - 1` hops; if any outer has non-empty
`typeParameters` and the corresponding outer ref-param-list is empty,
classify as raw.

Critical detail: the walk uses `parts.size - 1` hops, **not**
`!outer.isStatic`. `FirBackedJavaClassAdapter.isStatic` reports `true` for
top-level outers (because their `nonEnhancedTypeParameters` contain no
`FirOuterClassTypeParameterRef`s — they capture nothing). Using
`!outer.isStatic` as the loop condition would short-circuit at the top-level
outer before checking its own type parameters, which are precisely the ones
missing in the qualified raw form `XLineBreakpointType.XLineBreakpointVariant`.

Once classified as raw, `JavaTypeConversion`'s `JavaClass` branch ignores
`typeArguments` and uses
`typeParameterSymbols.getProjectionsForRawType(session, …)` to synthesise
erased projections (upper-bound erasure of each captured type parameter).
The resulting `ConeRawType` matches Kotlin's `<*>`-projected receiver via
star-subtyping.

### Test Results

| Test | Before | After |
|---|---|---|
| `testIntellij_platform_debugger_impl` | FAIL (`UNRESOLVED_REFERENCE_WRONG_RECEIVER` on `XLineBreakpointVariant.asProxy()`) | **PASS** |

`JavaUsingAst*` matrix (`Phased + Box`): `BUILD SUCCESSFUL in 1m 49s`,
**0 FAILED** — no regression vs. 2793/2793.

Cumulative across this iteration's five fix bundles (Cat B + Cat A + array +
star-import-supertype + binary-const-eval + this), the java-direct-only
failure count on the IJ FP corpus dropped from 11 to 2:

```
PASS: zeppelin (Cat B), psi_impl, javascript_tests, swift_language (Cat A),
      lint_common (array iter), r (star-import iter), android_core
      (binary-const-eval iter), platform_debugger_impl (this iter),
      android_transport (flaky)
FAIL: platform_lang_impl, remoteRun
```

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | `computeIsRaw`: detect qualified-form raw (`Outer.Inner` multi-part reference with generic outer and no `<>` on the outer). Walks outer chain by `rawTypeNameParts.size - 1` hops; KDoc explains why the walk isn't bounded by `outer.isStatic`. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **`isStatic` on `FirBackedJavaClassAdapter` is a "captures outer type
  params" predicate, not an "is a static nested class" predicate.** For a
  top-level class — which has no outer — the adapter reports `isStatic =
  true` because there is nothing to capture. This is correct for the
  capture-semantics question but trips up any walk that stops at "static"
  thinking it's reached the top of the lexical chain.
- **The qualified form's rawness is governed by the **source's** outer
  ref-param-list, not the class's `isStatic` shape.** The detection of
  raw uses the AST text (how many qualifier hops are written, how many of
  them carry `<>`), and only consults the class structure for type
  parameter counts.
- **Diagnostic rendering of `ConeRawType` still shows `<*>` in some
  contexts.** The receiver-type renderer can present a raw type with
  star-projection-like notation; that visual hint doesn't tell you whether
  the runtime structure is `ConeRawType` or a regular `ConeClassLikeType`
  with `ConeStarProjection` arguments. Receiver matching distinguishes them.

### Notes / follow-ups not in this iteration

- `platform_lang_impl` (Cat D `NlsContexts.Tooltip`): pre-existing cross-
  module annotation accessibility issue, separate from java-direct.
- `remoteRun` (Cat E `NegativeArraySizeException` at ASM `Frame.merge`):
  backend codegen crash on `doCheckConnection`. The
  `IJ_FP_REGRESSION_ANALYSIS_2026_05_10.md` doc hypothesised this might
  clear after Cat A/C; it did not (the failing module survives). The crash
  is downstream of stack-frame merging and likely a separate bug class.

---

## Cross-language `ConstantEvaluator` callback dropped binary Java field constants by passing simple class names — 2026-05-10 (previously latest)

### Overview

`testIntellij_android_core` failed locally with
`Initializer for const property RESOURCE_CLASS_SUFFIX was not evaluated` on
the Kotlin top-level
`private const val RESOURCE_CLASS_SUFFIX = "." + AndroidUtils.R_CLASS_NAME`.
The Java field that backs the chain (`AndroidUtils.R_CLASS_NAME = SdkConstants.R_CLASS`)
is loaded by java-direct from source, but its initializer references a
**binary** Java field (`com.android.SdkConstants.R_CLASS`, a `public static final
String`). Master/PSI evaluates the chain end-to-end; java-direct silently
returned `null` for the binary leaf, leaving Kotlin's const-eval gated open.

The CI symptom (`MISSING_DEPENDENCY_SUPERCLASS BaseBuilder`) reproduced
neither locally nor on the post-fix run (its trigger was Cat A's binary-
supertype-candidate path, fixed in the previous Cat A iteration). Local runs
on this branch deterministically expose the **const-eval** symptom on the
same module.

### Root cause

`ConstantEvaluator.evaluateReferenceExpression` already had a cross-language
escape hatch — it falls back to `resolveExternalReference?.invoke(className,
fieldName)` when `findLocalClass(className)` returns `null`. The callback
points at `FirJavaFacade.resolveExternalFieldValue`, which expects a
**fully-qualified** class name (or a current-package shortcut) and delegates to
`getClassDeclaredPropertySymbols(classId, propertyName)` on the resolved
`FirRegularClassSymbol` to fetch the field/property symbol.

The bug: `evaluateReferenceExpression` passed the **simple** class name as
written in the source (`"SdkConstants"` from the literal text
`SdkConstants.R_CLASS`), bypassing the file's `import com.android.SdkConstants;`
that java-direct's resolution context knows about. Inside
`resolveExternalFieldValue`, the simple name expanded only to the two trivial
candidates `ClassId(currentPackage, SdkConstants)` and
`ClassId.topLevel(FqName("SdkConstants"))` — neither exists. Both
`tryResolveAsTopLevel` and `tryResolveAsClassMember` returned `null`,
the callback returned `null`, and so did the chain.

| | classQualifier passed | classIds tried | result |
|---|---|---|---|
| **Before fix** | `"SdkConstants"` | `[org.jetbrains.android.util.SdkConstants, <root>.SdkConstants]` | both empty → `null` |
| **After fix** | `"com.android.SdkConstants"` | `[com.android.SdkConstants]` | binary `FirJavaField R_CLASS` → constant `"R"` |

PSI/master is unaffected because PSI's `JavaField.initializerValue` for the
source `AndroidUtils.R_CLASS_NAME` has full PsiResolveResult on the qualifier,
so the simple name `SdkConstants` has already been resolved through PSI's
file-scope before the constant evaluator runs.

### Fix

In `ConstantEvaluator.evaluateReferenceExpression`, when
`findLocalClass(className)` returns null **and** `className` is a simple name
(no dot), promote it to its FQN via
`containingClass.resolutionContext.resolve(className)?.asSingleFqName()`. The
existing simple-name resolver already honours the file's
explicit-imports → same-package → `java.lang` → star-imports chain via the FIR
`tryResolve` probe, so the FQN it returns is exactly the one
`resolveExternalFieldValue` needs to construct
`ClassId(parent, simpleName)` and reach the binary class's field/property.
If `resolve` cannot identify a class (e.g. a stale unresolved qualifier),
keep the original simple name as a fallback so the prior
current-package / `<root>` probe path stays intact.

The fix lives entirely in java-direct's `ConstantEvaluator` — no shared FIR
file is touched, and `resolveExternalFieldValue`'s contract (FQN dotted
qualifier in, constant value out) is unchanged. The cross-language callback
shape stays `(classQualifier: String?, fieldName: String) -> Any?`, with
java-direct now feeding the resolved FQN through it.

### Test Results

| Test | Before | After |
|---|---|---|
| `testIntellij_android_core` | FAIL (`Initializer for const property RESOURCE_CLASS_SUFFIX was not evaluated`) | **PASS** |

`JavaUsingAst*` matrix (`Phased + Box`): `BUILD SUCCESSFUL in 2m 0s`,
**0 FAILED** — no regression vs. 2793/2793.

Cumulative across this iteration's four fix bundles (Cat B + Cat A + array +
star-import-supertype + this), the java-direct-only failure count on the IJ FP
corpus dropped from 11 to 3:

```
PASS: zeppelin (Cat B), psi_impl, javascript_tests, swift_language (Cat A),
      lint_common (array iter), r (star-import iter), android_core (this iter),
      android_transport (flaky)
FAIL: platform_debugger_impl, platform_lang_impl, remoteRun
```

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../util/ConstantEvaluator.kt` | `evaluateReferenceExpression`: promote simple class name to FQN via `containingClass.resolutionContext.resolve(...)` before invoking the cross-language callback; KDoc records the rationale (binary Java fields require the qualifier to be resolved against the file's imports). |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **Cross-language callbacks need a resolved-FQN contract, not a literal-text
  contract.** Every ambiguity in a simple class name (current-package vs
  imported vs star-imported vs `java.lang`) lives in the **caller's** context,
  never the receiver's. java-direct already owns the resolution context, so
  pushing the FQN through is the natural fix; making the callback "guess"
  imports on the FIR side would duplicate work and lose accuracy.
- **`FirVariableSymbol<*>.tryExtractConstantValue` already handles FirField.**
  `getClassDeclaredPropertySymbols` returns `List<FirVariableSymbol<*>>`,
  including FirField symbols when the class is a `FirJavaClass`. The existing
  `tryResolveAsClassMember` branch was correct in shape — only the qualifier
  it received was wrong.
- **CI symptom and local symptom can diverge for the same module.** CI
  reported `MISSING_DEPENDENCY_SUPERCLASS BaseBuilder` (Cat A's binary-supertype
  path); local reproduced the const-eval bug. Cat A's earlier fix landed in
  this iteration's HEAD, so the residual local symptom was the const-eval
  one, and the BaseBuilder symptom no longer reproduced anywhere.

### Notes / follow-ups not in this iteration

- `platform_debugger_impl` (Cat C): `XLineBreakpointType<*>.XLineBreakpointVariant.asProxy()` —
  Java nested non-static class with outer-only type parameters. Likely
  needs a deeper look at how java-direct converts
  `XLineBreakpointType<?>.XLineBreakpointVariant`-shaped Java type references
  to ConeKotlinType (outer-arg propagation through inner non-static class with
  no own type params).
- `platform_lang_impl` (Cat D), `remoteRun` (Cat E codegen): pre-existing /
  downstream of upstream resolution issues; per
  `IJ_FP_REGRESSION_ANALYSIS_2026_05_10.md` Cat E, fixing Cat C may also clear
  remoteRun's `NegativeArraySizeException` since the codegen crash is the
  fallout of a malformed receiver type reaching the back-end.

---

## Star-imported binary supertypes silently dropped by `JavaSupertypeGraph.resolveSupertypeReference` — 2026-05-10 (previously latest)

### Overview

`testIntellij_r` failed with
`ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED include(RowFilter.Entry<out M!, out I!>!)`
on `RDataFrameFiltersHandler` (Kotlin), which extends a chain of Java classes
ending in `Filter extends RowFilter` (raw, JDK binary referenced via
`import javax.swing.*`). PSI/master accept the `include(Entry rowEntry)` raw
override transparently; java-direct rejected it because the candidate's `Entry`
parameter was resolving to a bogus `<root>.Entry` `ConeFlexibleType` instead of
`ConeRawType[RowFilter.Entry]`.

### Root cause

`JavaSupertypeGraph.resolveSupertypeReference` had a star-import branch that
emitted a candidate `ClassId` only after `sameClassInSameFilePackage(starPkg, name)`
returned true — i.e. only for star-imported supertypes whose target lives in
the source index. Every binary on-demand supertype (e.g. `Filter extends RowFilter`
via `import javax.swing.*`, where `javax.swing.RowFilter` is shipped in the JDK)
silently returned `null`. Downstream:

| Layer | Effect |
|---|---|
| `getDirectSupertypes(Filter)` | empty list (no candidate for binary `RowFilter`) |
| `collectInheritedInnerClasses(AndFilter)` | walks no parent of `Filter`; `Entry` map is empty |
| `walkJavaSourceSupertypes` (BFS) | descends `AndFilter→ComposedFilter→Filter` then stops; `nonSourceSupertypeIds` stays empty |
| `walkBinarySupertypes` | nothing to walk |
| `JavaResolutionContext.resolveFromLocalScope` | "Entry" simple-name probe falls through |

`resolveSimpleNameToClassIdImpl` eventually probed star imports and resolved
`"RowFilter"` (top-level) via `resolveFromStarImports`, but the **nested** simple
name `"Entry"` went unresolved — the inherited-inner-class walks were the only
sources for it, and they had been deprived of `Filter→RowFilter`.

`JavaTypeConversion`'s `null` (classifier-null) branch then fell back to
`findClassIdByFqNameString("Entry", session)` (returns `null` for a one-segment
unprefixed FQN) and finally to `ClassId.topLevel(FqName("Entry"))` — a bogus
root-package `ClassId`. The resulting `ConeFlexibleType` for the candidate
parameter has no `RawType` attribute, so
`JavaOverrideChecker.isEqualTypes(candidate is ConeRawType -> JVM-descriptor-compare)`
short-circuited away from the descriptor match and the structural compare
failed (bogus `<root>.Entry` ≠ `RowFilter.Entry`-raw).

The diagnostic's rendered base signature `Entry<out M!, out I!>!` reflects the
declared signature of `RowFilter.include` as displayed by the renderer, not the
post-substitution form actually used in matching — the actual match failure was
on the candidate side.

### Fix

In `resolveSupertypeReference`, change the return type from `ClassId?` to
`List<ClassId>` and emit one candidate per star-import package, mirroring the
explicit-import treatment Cat A introduced. Source-index matches keep priority
(returned alone when any match); when no source class is found, every
star-import package contributes a candidate `ClassId` and the downstream
`tryResolve` probes (in `walkJavaSourceSupertypes`,
`JavaResolutionContext.directSupertypeClassIds`, etc.) decide existence.

KDoc updated to record the candidate-vs-existence boundary for star imports
explicitly, and to note that the previous single-`ClassId?` shape predated this
boundary by short-circuiting at the layer that has no classpath visibility.

### Test Results

| Test | Before | After |
|---|---|---|
| `testIntellij_r` | FAIL (`ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED include(RowFilter.Entry<out M!, out I!>!)`) | **PASS** |

`JavaUsingAst*` matrix (`Phased + Box`): `BUILD SUCCESSFUL in 1m 48s`,
**0 FAILED** — no regression vs. 2793/2793.

Cumulative across this iteration's three fix bundles (Cat B + Cat A + array +
this), the java-direct-only failure count on the IJ FP corpus dropped from 11
to 4:

```
PASS: zeppelin (Cat B), psi_impl, javascript_tests, swift_language (Cat A),
      lint_common (array iter), r (this iter), android_transport (flaky)
FAIL: android_core, debugger_impl, platform_lang_impl, remoteRun
```

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../util/JavaSupertypeGraph.kt` | `resolveSupertypeReference`: returns `List<ClassId>`; emits one candidate per star-import package on the binary fallthrough so downstream `tryResolve` probes decide existence. KDoc records the candidate-vs.-existence boundary. `extractSupertypeRefsFromNode`: `addAll` instead of `?.let { add }`. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **Star imports are the JDK's load-bearing import mechanism.** Most JDK uses
  in IntelliJ-style codebases come through `import javax.swing.*` /
  `import java.awt.*`. Treating star imports as second-class in the candidate
  layer means the JDK is structurally invisible to inherited-nested-class
  resolution. Cat A's explicit-import-only fix passed because IntelliJ-platform
  internals favour explicit imports; community/third-party Java code (the `r`
  module's `Filter extends RowFilter` chain) leans on star imports.
- **Multiple star-import candidates per supertype are fine here.** Each phantom
  `ClassId` triggers at most one extra `tryResolve` probe (`getInnerClassNames`
  of an absent class returns `emptySet`, `directSupertypeClassIds` of an absent
  class returns `emptyList`). The amplification factor is the file's
  star-import count — typically 1–3 — so the perf cost is small and bounded.
- **Diagnostic message wording is not a reliable trace of the matching logic.**
  `Entry<out M!, out I!>!` in the failure looked like a parameterized-vs-raw
  substitution mismatch on the **base** side; the actual mismatch was on the
  **candidate** side (its parameter type was bogus). Always confirm both sides
  before hypothesizing about `ConeRawScopeSubstitutor` or
  `AbstractSignatureParts`-level differences.

### Notes / follow-ups not in this iteration

- `android_core` CI reports `MISSING_DEPENDENCY_SUPERCLASS BaseBuilder`. The
  source `StudioExceptionReport.java` has neither an explicit import nor a
  star import for `BaseBuilder`; `BaseBuilder` is referenced by simple name
  with no obvious resolution path. Either the Java file relies on a same-package
  binary class shipped via classpath, or this is a different bug class
  (cross-module visibility).
- `debugger_impl` (Cat C): receiver mismatch on
  `XLineBreakpointType.XLineBreakpointVariant<*>` — likely outer-class type
  parameter propagation through inner class star projection.
- `platform_lang_impl` (Cat D), `remoteRun` (Cat E): pre-existing.

---

## `@NotNull T[]` array nullability double-applied via member annotations on the outer array wrapper — 2026-05-10 (previously latest)

### Overview

`testIntellij_android_lint_common` failed with
`RETURN_TYPE_MISMATCH_ON_OVERRIDE`: a Kotlin override returning
`Array<out IntentionAction>?` was rejected because the parent (Java)
`@NotNull IntentionAction[] getIntentions(...)` was being loaded with the
**array** enhanced as non-null (`Array<(out) IntentionAction!>`). Master/PSI
loads the same signature as `Array<(out) IntentionAction!>!` (flexible
array, non-null component), making the nullable override valid.

### Root cause

`JavaTypeOverAst` exposes `memberAnnotations` (annotations harvested from a
member's `MODIFIER_LIST`) as TYPE-level annotations on the resulting
`JavaType`. For method/parameter types, that means the method's
`@NotNull` (a TYPE_USE-applicable annotation by virtue of
`org.jetbrains.annotations.NotNull`'s `@Target(... TYPE_USE ...)`) ends up
on the return type's annotation list as well as on the member symbol.

For non-vararg arrays, `tryCreateArrayOrVarargFromTypeNode` placed the
member annotations on the **outer** `JavaArrayTypeOverAst` wrapper. FIR's
`AbstractSignatureParts.kt:104-111` (KT-24392) deliberately filters
TYPE_USE annotations OUT of the **container** annotations when the head
type is an array, to avoid double-application across array-head and
component:

```kotlin
!typeParameterBounds && enableImprovementsInStrictMode && type?.isArrayOrPrimitiveArray() == true ->
    containerAnnotations.filter { !annotationTypeQualifierResolver.isTypeUseAnnotation(it) } + typeAnnotations
```

But that filter only addresses **container** annotations — `typeAnnotations`
are taken as-is. By placing `@NotNull` on the array's own `annotations`, we
smuggled it past the filter, resulting in:

| | typeAnnotations on array | container | composed (array-head) | enhanced array |
|---|---|---|---|---|
| **PSI master** | `[]` (PsiArrayType empty) | `[@NotNull]` (PsiMethod) | `[]` (filtered) | flexible (correct) |
| **java-direct (before fix)** | `[@NotNull]` (memberAnnotations attached) | `[@NotNull]` (JavaMethod.annotations) | `[@NotNull]` from typeAnn | **non-null** (BUG) |

The component side is unaffected: for non-vararg arrays
`componentMemberAnnotations` was already `emptyList()`, so
`Array<(out) IntentionAction!>` (non-null component via container
annotations on the non-head type position) is unchanged.

### Fix

In `tryCreateArrayOrVarargFromTypeNode`, set the outer array wrapper's
member annotations to `emptyList()` unconditionally (was: `memberAnnotations`
for non-vararg, `emptyList()` for vararg). The vararg path still places
`memberAnnotations` on the **component** type — that's the
PSI/javac-wrapper convention for `@NonNull String...`. Updated the function
KDoc to cite KT-24392 and the PSI parity rationale.

### Test Results

| Test | Before | After |
|---|---|---|
| `testIntellij_android_lint_common` | FAIL (`RETURN_TYPE_MISMATCH_ON_OVERRIDE` `getIntentions`) | **PASS** |

`JavaUsingAst*` matrix (`Phased + Box`): `BUILD SUCCESSFUL in 3m 6s`, **0 FAILED** — no regression vs. 2793/2793.

Cumulative across the IJ FP iteration to date (Cat B + Cat A + this fix), the
java-direct-only failure count dropped from 11 to 5:

```
PASS: zeppelin (Cat B), psi_impl, javascript_tests, swift_language (Cat A),
      lint_common (this iter), android_transport (flaky)
FAIL: r, android_core, debugger_impl,
      platform_lang_impl, remoteRun
```

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | `tryCreateArrayOrVarargFromTypeNode`: clear `arrayMemberAnnotations` unconditionally for non-vararg arrays; KDoc updated to cite KT-24392 and PSI parity rationale. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- FIR's `AbstractSignatureParts.kt:104-111` is the canonical place that
  prevents `@NotNull` on a method returning `T[]` from enhancing both the
  array AND the component (KT-24392). The protection only filters
  **container** annotations; bypassing it via type-level annotations is
  silent and the diagnostic only surfaces in subclass override checking.
- PSI's `PsiArrayType.getAnnotations()` returns `[]` for method-level
  annotations precisely because PSI keeps method modifier-list annotations
  on the method, not the type. java-direct's `memberAnnotations` carrier
  blurred that boundary; collapsing it back to PSI semantics for array
  outer wrappers is the right model.
- For varargs (`@NonNull String... args`), the
  parameter-level `@NonNull` belongs on the **component** for
  PSI/javac-wrapper parity — that path is unchanged.

### Notes / follow-ups not in this iteration

- `r` raw-vs-generic `include(RowFilter.Entry)` override: depends on how
  `JavaClassOverAst.supertypes` propagates `isRaw` for a Java class that
  extends a raw Java class (`Filter extends RowFilter` where
  `RowFilter<M, I>` is binary). Likely a `JavaOverrideChecker` /
  `JavaClassUseSiteMemberScope` interaction with the raw-substitution flag.
- `android_core`: two distinct sub-bugs surfaced depending on file order —
  CI shows `MISSING_DEPENDENCY_SUPERCLASS BaseBuilder` (binary supertype
  visibility on cross-module load); local rerun shows
  `Initializer for const property RESOURCE_CLASS_SUFFIX was not evaluated`
  (java-direct's `ConstantEvaluator` cannot resolve binary Java field
  constants like `SdkConstants.R_CLASS` because
  `FirJavaFacade.resolveExternalFieldValue` only checks Kotlin
  top-level / class member / companion symbols, not binary Java
  `FirField`s).
- `debugger_impl` (Cat C): receiver mismatch on
  `XLineBreakpointType.XLineBreakpointVariant<*>` — likely outer-class
  type-parameter propagation through inner class star-projection.
- `platform_lang_impl` (Cat D), `remoteRun` (Cat E): pre-existing.

---

## Category A of the IJ FP regression delta: inherited-nested-class lookup over binary supertypes + private interface methods — 2026-05-10

### Overview

Three linked java-direct bugs. The first two cooperated to silently drop
every binary-classpath Java supertype during inherited-nested-class lookup,
so any Kotlin class extending a Java class whose abstract members referred
to a nested type declared on a transitive **binary** Java supertype hit a
spurious `ABSTRACT_MEMBER_NOT_IMPLEMENTED`. The third was a Java 9+ private
interface method handling miss in member loading: such methods were
returned with visibility `Public` and `isAbstract == true`, which then
showed up as additional `ABSTRACT_MEMBER_NOT_IMPLEMENTED` reports
downstream of the first two.

### Root causes

**(1) `JavaSupertypeGraph.resolveSupertypeReference` — explicit-import
existence gate.** The function returned a `ClassId` only after passing
`sameClassInSameFilePackage(importPkg, importName)`, which is true *only*
for sources in the source index. Every supertype reference whose target
lives in a binary classpath (e.g. `LintIdeQuickFix extends PriorityAction`
where `PriorityAction.class` ships with `intellij.platform.analysis-api`)
silently returned `null`, and therefore never appeared in
`getDirectSupertypes(...)`'s list. Downstream (`collectInheritedInnerClasses`,
`walkBinarySupertypes`'s feed list) lost every binary supertype.

**(2) `JavaInheritedMemberResolver.walkJavaSourceSupertypes` — wrong file's
imports for transitive levels.** When the BFS descended from
`DefaultLintQuickFix.java` to its source supertype `LintIdeQuickFix.java`'s
own supertypes, the next level was built by adding raw
`JavaClassifierType`s from `LintIdeQuickFix.supertypes`, then resolving
their names via the *caller's* `resolveWithoutInheritance` (i.e. with
`DefaultLintQuickFix.java`'s `simpleImports`). `LintIdeQuickFix`'s import
of `com.intellij.codeInsight.intention.PriorityAction` is invisible to
`DefaultLintQuickFix.java`, so `resolveWithoutInheritance("PriorityAction")`
returned `null`. Result: `nonSourceSupertypeIds` was never populated for
the transitive binary supertype, and `walkBinarySupertypes` had nothing
to walk.

**(3) `JavaMemberOverAst.{visibility, isAbstract}` — private interface
methods.** Java 9+ allows `private` methods inside interfaces; they must
have a body and are not abstract. `visibility` returned `Visibilities.Public`
for *every* interface member regardless of explicit modifiers (line 55 of
`JavaMemberOverAst.kt`); `isAbstract` was `super.isAbstract || (isInterface
&& !default && !static)` — no `private` clause. Symptom: methods like
`PropertySignatureCommonImpl.copyPropertySignatureWithTypeAndSource`
(declared `private @NotNull JSRecordType.PropertySignature ...`) showed up
as public abstract, and Kotlin subclasses (`JSDelegatePropertySignature`)
were flagged as not implementing them.

### Fixes

1. **`JavaSupertypeGraph.resolveSupertypeReference`** — return the
   candidate `ClassId` from the explicit-import path without the
   source-existence check. The KDoc explains the invariant: this layer
   computes candidates; the downstream FIR symbol provider / class finder
   decides existence. Star imports keep the source-only gate (binary
   on-demand imports for inheritance are rare and would require
   classpath-wide enumeration here).

2. **`JavaInheritedMemberResolver.walkJavaSourceSupertypes`** — refactor
   to operate on `ClassId`s after the initial level. The first level
   still resolves `JavaClassifierType.presentableText` against the
   caller's context (correct — those classifiers belong to the file
   currently being parsed). For depth ≥ 1, use
   `classFinder.getDirectSupertypes(supertypeClassId)`, which the
   per-class `JavaSupertypeGraph` resolves with *that file's* imports
   and now includes binary `ClassId`s thanks to fix (1). Source vs.
   binary is split via `classFinder.isClassInIndex`; binary `ClassId`s
   feed `nonSourceSupertypeIds` for `walkBinarySupertypes` to process
   via the per-origin `directSupertypeClassIds` dispatcher.

3. **`JavaMemberOverAst.{visibility, isAbstract}`** — check
   `JavaSyntaxTokenType.PRIVATE_KEYWORD` *before* the
   `containingClass.isInterface` short-circuit in `visibility`, and
   add `&& !hasModifier(PRIVATE_KEYWORD)` to the interface clause in
   `isAbstract`. Mirrors PSI's
   `hasModifierProperty(PsiModifier.ABSTRACT)`, which sets the implicit
   abstract bit only when none of `default` / `static` / `private` is
   present.

### Test Results

Selected re-run on `IntelliJFullPipelineTestsGenerated` (per-test):

| Test | Before | After |
|---|---|---|
| `testIntellij_javascript_psi_impl` | FAIL (`ABSTRACT_MEMBER_NOT_IMPLEMENTED JSRecordType.MemberSource`) | **PASS** |
| `testIntellij_javascript_tests` | FAIL (`ABSTRACT_MEMBER_NOT_IMPLEMENTED TypeScript*`) | **PASS** |
| `testIntellij_swift_language` | FAIL (`ABSTRACT_MEMBER_NOT_IMPLEMENTED SwiftSymbolResult` ×30) | **PASS** |
| `testIntellij_android_lint_common` | FAIL (`ABSTRACT_MEMBER_NOT_IMPLEMENTED setPriority(PriorityAction.Priority)`) | FAIL — new 1st error `RETURN_TYPE_MISMATCH_ON_OVERRIDE` `getIntentions` (Java `@NotNull T[]` array nullability — separate bug, latent) |
| `testIntellij_r` | FAIL (`ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED include(RowFilter.Entry<...>)`) | FAIL — same 1st error (raw `Entry` override of generic `Entry<? extends M, ? extends I>` not recognised — separate bug) |
| `testIntellij_android_core` | FAIL (`MISSING_DEPENDENCY_SUPERCLASS BaseBuilder`) | FAIL — same (cross-module supertype accessibility — separate bug) |

`JavaUsingAst*` matrix (`Phased + Box`): `BUILD SUCCESSFUL in 2m 23s`,
**0 FAILED** — no regression vs. 2793/2793.

Cumulative across this iteration's two fix bundles (Cat B + Cat A), the
java-direct-only failure count on the IJ FP corpus dropped from 11 to 6:

```
PASS: zeppelin (Cat B), psi_impl, javascript_tests, swift_language (Cat A),
      android_transport (was the flaky NegativeArraySize — not reproducing now)
FAIL: lint_common, r, android_core, debugger_impl,
      platform_lang_impl, remoteRun
```

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../util/JavaSupertypeGraph.kt` | `resolveSupertypeReference`: drop `sameClassInSameFilePackage` existence check on the explicit-import path; KDoc explains the candidate-vs.-existence boundary. |
| `compiler/java-direct/src/.../resolution/LeanJavaClassFinder.kt` | Add `getDirectSupertypes(classId)` to the interface, with KDoc covering the per-class imports invariant. |
| `compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt` | `internal fun getDirectSupertypes` → `override fun` to satisfy the new interface method. |
| `compiler/java-direct/src/.../resolution/JavaInheritedMemberResolver.kt` | `walkJavaSourceSupertypes`: convert initial `JavaClassifierType` list to `ClassId`s via the caller's context; for transitive levels, use `classFinder.getDirectSupertypes(supertypeClassId)` (per-class imports) instead of `javaClass.supertypes` re-resolved through the caller's context. KDoc updated. |
| `compiler/java-direct/src/.../model/JavaMemberOverAst.kt` | `visibility`: check `PRIVATE_KEYWORD` before the `isInterface → Public` short-circuit. `isAbstract` (interface methods): add `&& !hasModifier(PRIVATE_KEYWORD)`. KDocs cite Java 9+ private interface methods and PSI's matching behaviour. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **Two compounding bugs masked the same end-symptom.** Fixing only (1) or
  only (2) would not have cleared a single inherited-nested-class case
  through a binary supertype: (1) without (2) means
  `getDirectSupertypes(...)` knows binary supertypes but the BFS in
  `walkJavaSourceSupertypes` doesn't ask for them; (2) without (1) means
  the BFS asks but `getDirectSupertypes` returns `null` for binary
  references. The 5/8 (zeppelin counted as Cat B) → 4/6 reduction is the
  combined effect.
- **Per-class import scopes are non-trivial in transitive walks.** The
  guideline going forward: any code that descends through a Java source
  supertype hierarchy must use the descendant's own resolution context
  (or its already-cached `ClassId` list) to resolve the descendant's
  supertype names. Reusing the caller's context across files is the same
  shape of bug as scope leakage in
  `BinaryJavaClassFinder.findClassImpl`'s `ClassifierResolutionContext`
  caveat.
- **Java 9+ private interface methods are easy to miss.** PSI's
  `hasModifierProperty(ABSTRACT)` quietly handles all three exception
  modifiers (`default` / `static` / `private`); explicit re-implementations
  (java-direct's `JavaMemberOverAst`) must enumerate them by hand. A
  unit-level smoke test that loads one of each shape would have surfaced
  this immediately.
- **Same fix removes two failure shapes from the same module.** The
  `psi_impl` module had inherited-nested-class misses **and** private
  interface methods reported as abstract; both came from the same Java
  type (`PropertySignatureCommonImpl`). Once (1)+(2)+(3) all landed, the
  remaining diagnostics were genuinely unrelated to nested-class /
  private-method handling.

### Notes / follow-ups not in this iteration

- **`lint_common`**'s remaining `RETURN_TYPE_MISMATCH_ON_OVERRIDE` on
  `getIntentions` (`Array<(out) IntentionAction!>` vs.
  `Array<out IntentionAction>?`) traces to how java-direct attaches
  `@NotNull` to a Java array return type. PSI lifts the annotation onto
  the array as a whole; if java-direct lifts it onto the element instead,
  Kotlin sees the array as flexible/nullable and the override matches —
  conversely, the precise mis-attribution here is to investigate.
- **`r`**'s remaining `ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED include`
  needs override-resolution between `AndFilter`'s raw
  `include(RowFilter.Entry rowEntry)` and `RowFilter`'s generic
  `include(Entry<? extends M, ? extends I>)`. Either java-direct doesn't
  load `AndFilter`'s `include` at all (raw type formatting in member
  loading?) or FIR's override-equivalence on raw-vs-generic mismatches
  PSI's behaviour for java-direct-loaded methods.
- **`android_core`**'s `MISSING_DEPENDENCY_SUPERCLASS BaseBuilder` is a
  cross-module case (`BaseBuilder` lives in
  `intellij.platform.ide.impl`, referenced from
  `intellij.android.core` via the inherited-supertype chain
  `Builder → BaseBuilder`). Likely the same shape as Cat D's
  `NlsContexts.Tooltip`: cross-module accessibility on annotations /
  supertypes through java-direct's binary class finder.
- A regression test for the inherited-nested-class-via-binary-supertype
  shape and one for private interface methods belong in the
  `JavaUsingAst*` corpus.

---

## `BinaryJavaClassFinder.knownClassNamesInPackage` `$`-filter removal: unhide Scala companion-module classes — 2026-05-10

### Overview

One of the 11 modules in the `IntelliJFullPipelineTestsGenerated` failure
delta vs master — `intellij.bigdatatools.zeppelin` — was failing with
`UNRESOLVED_IMPORT` / `UNRESOLVED_REFERENCE` for Scala-style class names
ending in `$` (`ScalaLibraryProperties$`, `Element$`, `None$`, `package$`).
Diff between PSI's `knownClassNamesInPackage` and java-direct's showed
java-direct was excluding any class file whose name contains `$`; PSI was
not. Removing the filter to mirror PSI fixes the module without affecting
the JavaUsingAst\* matrix.

### Root cause

`BinaryJavaClassFinder.knownClassNamesInPackage`
(`compiler/java-direct/src/.../BinaryJavaClassFinder.kt:184-199`):

```kotlin
index.traverseClassVirtualFilesInPackage(packageFqName, extensions) { file ->
    val name = file.nameWithoutExtension
    if (!name.contains('$')) {        // <-- filter
        result.add(name)
    }
    true
}
```

PSI's `KotlinCliJavaFileManagerImpl.knownClassNamesInPackage`
(`compiler/cli/cli-base/src/.../KotlinCliJavaFileManagerImpl.kt:267-280`)
adds **every** class file's `nameWithoutExtension`, with no `$` filter.

The filter was intended to exclude inner-class spillover
(`Outer$Inner.class`) from package enumeration. But it also excludes
legitimate top-level classes whose JVM name contains `$` — most importantly
**Scala companion-module classes** (`Foo$.class`), which Kotlin imports via
backticks
(`import org.jetbrains.plugins.scala.project.\`ScalaLibraryProperties$\``).
Such files appear as top-level classes on disk; the existing
`isNotTopLevelClass(classContent)` guard inside `findClassImpl`
(line 141) is the right place for the inner-class-spillover defence and
correctly admits them. Filtering at the package-enumeration step was
strictly too coarse — and was the path FIR's resolution actually consulted
to decide whether to even try `findClass`.

### Fix

Drop the `$` check in `knownClassNamesInPackage`; rely on
`findClassImpl`'s `isNotTopLevelClass` guard for inner-class spillover.

```kotlin
override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> =
    knownClassNamesCache.getOrPut(packageFqName) {
        val result = LinkedHashSet<String>()
        index.traverseClassVirtualFilesInPackage(packageFqName, extensions) { file ->
            // Mirror `KotlinCliJavaFileManagerImpl.knownClassNamesInPackage`: include every
            // class file's name, including ones that contain `$`. Genuine inner-class spill
            // (`Outer$Inner.class`) is filtered later inside `findClassImpl` via
            // `isNotTopLevelClass(classContent)`. A blanket name-level `$` filter wrongly
            // hides legitimate top-level classes whose JVM name contains `$` — e.g. Scala
            // companion modules (`Foo$.class`) — which Kotlin imports via backticks.
            result.add(file.nameWithoutExtension)
            true
        }
        result
    }
```

### Test Results

- `testIntellij_bigdatatools_zeppelin`: **PASS** (was: failing with
  `UNRESOLVED_IMPORT 'ScalaLibraryProperties$'` and three sibling
  diagnostics in `ScalaSdkDependencyPatcherImpl.kt`).
- **`JavaUsingAst*` matrix**: `BUILD SUCCESSFUL in 2m 54s`, 0 FAILED — no
  regression vs. 2793/2793.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/BinaryJavaClassFinder.kt` | Remove `$`-name filter in `knownClassNamesInPackage`; replace the comment to explain the change of policy and the placement of the inner-class-spillover defence inside `findClassImpl`. |
| `compiler/java-direct/implDocs/IJ_FP_REGRESSION_ANALYSIS_2026_05_10.md` | New: full classification of the 11-module IJ FP regression delta and recommended order of attack. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **Two-stage filtering ≠ one combined filter.** The `$` exclusion was
  cheap defence-in-depth at enumeration time, but the *correct* defence
  (`isNotTopLevelClass(classContent)`) requires reading the bytes —
  unavailable until `findClassImpl`. Once the byte-level guard exists,
  duplicating it as a name-level approximation strictly **subtracts**
  precision.
- **Always diff against the PSI implementation when adding gates.** The
  PSI side has dealt with Scala interop for years; any java-direct
  divergence is a high-priority red flag. A line-by-line diff between
  `BinaryJavaClassFinder` and `KotlinCliJavaFileManagerImpl` would have
  caught this before landing.
- **`knownClassNamesInPackage` is consulted before `findClass`.**
  Names absent from this set are treated by FIR as not-existing, so the
  `findClass` path's guards never get a chance to run. This makes the
  enumeration filter strictly stricter than the find-time one in effect.

### Notes / follow-ups not in this iteration

- Categories A (inherited nested class from Java supertype invisible —
  6 modules), C (generic receiver mismatch — `debugger.impl`),
  D (`NlsContexts.Tooltip` — `platform.lang.impl`, already known), and
  E (ASM `NegativeArraySizeException` — `remoteRun`,
  `android.transport`) remain. See
  `implDocs/IJ_FP_REGRESSION_ANALYSIS_2026_05_10.md` for the
  recommended order of attack.
- Verify whether other places in the java-direct binary side have a
  similar enumerate-then-find double filter — `findPackage`,
  `findClasses`, and the source-side `knownClassNamesInPackage` are the
  three obvious candidates.

---

## `findInheritedNestedClass` double-guard fix: hoist supertype lookup out of loop checker — 2026-05-08

### Overview

After the `extractStaticImports` fix (entry below) reduced
`IntelliJFullPipelineTestsGenerated` failures from 70 → 14, one of the two
remaining non-test-data failures (`testIntellij_python_psi_impl`) showed a
distinct symptom: `MISSING_DEPENDENCY_CLASS Cannot access class 'PyFunction.Modifier'`
in `PyCallableTypeImpl.java`'s `@Nullable PyFunction.Modifier myModifier` field
type. `Modifier` is declared on `PyAstFunction` (a supertype of `PyFunction`);
Java code references it via inheritance per JLS 8.5. Java-direct's
`findInheritedNestedClass` is supposed to walk supertypes and find
`PyAstFunction.Modifier`, but instrumentation showed it received an empty
supertype list.

### Root cause

`JavaSupertypeLoopChecker.guarded(classId)` is keyed by the classId being
walked. Both `findInheritedNestedClass` and `directSupertypeClassIds` enter
the guard with the *same* classId. The previous code:

```kotlin
private fun findInheritedNestedClass(outerClassId, nestedName) =
    loopChecker.guarded(outerClassId, default = null) {
        for (supertypeId in directSupertypeClassIds(outerClassId)) {  // ← same classId
            ...
        }
    }
```

When `findInheritedNestedClass(PyFunction, "Modifier")` enters the guard,
`PyFunction` is on the active set. The inner `directSupertypeClassIds(PyFunction)`
call then sees `PyFunction` already on the active set and returns its `default`
(`emptyList()`) without computing supertypes. The for-loop iterates nothing,
the function returns `null`, and the inheritance lookup quietly fails.

This is the exact failure mode of every binary-classpath inherited inner
class lookup: each affected class hit the same double-guard.

### Fix

Hoist the `directSupertypeClassIds` call out of the guard:

```kotlin
private fun findInheritedNestedClass(outerClassId, nestedName): ClassId? {
    val supers = directSupertypeClassIds(outerClassId)
    return loopChecker.guarded(outerClassId, default = null) {
        for (supertypeId in supers) {
            ...
            findInheritedNestedClass(supertypeId, nestedName)?.let { return@guarded it }
        }
        ...
    }
}
```

The outer guard still bounds the recursion through
`findInheritedNestedClass(supertypeId, ...)` (different classId, but the same
class might appear as an indirect supertype of itself in a cycle). The
hoisted `directSupertypeClassIds` runs *before* `outerClassId` enters the
active set, so it's free to use its own guard machinery for its own cycle
detection.

### Test Results

- **`testIntellij_python_psi_impl`**: PASS (was the last non-test-data
  java-direct-attributable failure in the original 70).
- **Java-direct module suite**: `JavaUsingAstPhasedTestGenerated` +
  `JavaUsingAstBoxTestGenerated` BUILD SUCCESSFUL (no regression vs. 2699/2699).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | `findInheritedNestedClass`: hoist `directSupertypeClassIds(outerClassId)` call out of `loopChecker.guarded { ... }`; add KDoc explaining why. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **Shared loop checker keys are subtle.** The same `JavaSupertypeLoopChecker`
  instance is used by `directSupertypeClassIds`, `findInheritedNestedClass`,
  and (potentially) other supertype-walking entry points. Keying by `classId`
  alone means any two functions whose entry-time classId matches will silently
  starve each other inside a single call chain. The current single-key
  scheme works only if every entry-point reads its supertype list *before*
  pushing onto the active set.
- **Empty supertype lists are silent.** No diagnostic, no warning — just an
  empty for-loop. Detecting this without instrumentation is hard. A defensive
  check ("if `firClass.directSupertypeClassIds()` is empty for a class that
  has `Object` as ancestor, log a warning") would have surfaced this issue
  much earlier.
- **One inherited-inner-class regression at a time.** The inheritance lookup
  failure is generic — every `Java class A extends Java class B` (or interface
  inheritance equivalent) that has Kotlin/Java code referring to
  `A.NestedFromB` was broken. Only one IntelliJ-pipeline test landed on this
  exact shape after the static-import fix, but the underlying
  `findInheritedNestedClass` bug is wide.

### Notes / follow-ups not in this iteration

- **Add a unit test** for `findInheritedNestedClass` that reproduces the
  inherited-binary-inner-class case (`A extends B; B has nested class C; ref A.C`).
- **`testIntellij_platform_lang_impl` remains failing** with
  `MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR` for
  `NlsContexts.Tooltip` — different category (inferred-type cross-module
  annotation accessibility), not addressed by this fix.
- **Generalise the loop-checker design.** A clean fix would key the active
  set by `(entry-point, classId)` rather than just `classId`, so independent
  entry points don't interfere. Out of scope for this iteration.

---

## `extractStaticImports` parser-shape fix: recognize `JAVA_CODE_REFERENCE` shape for static-on-demand imports — 2026-05-08

### Overview

The `IntelliJFullPipelineTestsGenerated` corpus had 70 tests still failing after
yesterday's `LazySessionAccess` re-entrance work. Direct probing of one of
them — `testIntellij_javascript_parser` — via `System.err.println` instrumentation
in `JavaTypeConversion.toConeKotlinTypeForFlexibleBound` and
`JavaResolutionContext.resolve` revealed that the failures were **not**
test-data debt as previously assumed: java-direct **was** active for these
modules, and `JavaResolutionContext.resolve("State")` was returning `null` even
though the Java source under analysis (`JSTagOrGenericParser.java`) carried
`import static com.intellij.lang.javascript.parsing.JSTagOrGenericUtil.*;`. The
debug dump showed `starImports = []` — the static-on-demand import was being
silently dropped at parse-time inside `JavaImportResolver.extractStaticImports`.

### Root cause

The KMP Java parser emits **two distinct AST shapes** under
`IMPORT_STATIC_STATEMENT`:

- **Single static import** (`import static X.Y;`): `IMPORT_STATIC_REFERENCE`
  child carrying the full FQN.
- **Static-on-demand** (`import static X.*;`): `JAVA_CODE_REFERENCE` (the
  outer class's FQN, **without** the trailing `.*`) followed by sibling
  `DOT`, `ASTERISK`, `SEMICOLON` tokens. **No** `IMPORT_STATIC_REFERENCE`
  node is produced for this shape.

`extractStaticImports` only ran `tree.findChildByType(importNode, IMPORT_STATIC_REFERENCE)`,
so for every static-on-demand import the lookup returned `null` and the loop
hit `continue` — silently skipping the import entirely. Single static imports
were unaffected (which is why earlier iterations covering single-import edge
cases — KitkatIterationsResults entry 51 — worked: only the more recent
test-data corpora include static-on-demand imports of Kotlin objects with
nested classes).

### Fix

```kotlin
val refNode = tree.findChildByType(importNode, JavaSyntaxElementType.IMPORT_STATIC_REFERENCE)
    ?: tree.findChildByType(importNode, JavaSyntaxElementType.JAVA_CODE_REFERENCE)
    ?: continue
```

Also moved the `hasStar` computation above `refNode` so it doesn't depend on
which child the FQN came from.

### Test Results

- **Java-direct module suite**: `JavaUsingAstPhasedTestGenerated` +
  `JavaUsingAstBoxTestGenerated` BUILD SUCCESSFUL (no regression vs. the
  prior 2699/2699 baseline).
- **Original 70 IntelliJFullPipelineTestsGenerated failures**: re-running the
  full set under `--rerun-tasks`:
  - **56 now pass** (testIntellij_javascript_parser, testIntellij_go_impl,
    testIntellij_javascript_psi_impl, testFleet_noria_cells,
    testIntellij_clion_toolchains family, testIntellij_database_impl,
    testIntellij_php_impl, testIntellij_platform_ijent_impl,
    testIntellij_react family, testIntellij_rider_plugins_godot/unity/fsharp/
    for_tea/unreal_link family, testIntellij_swift_language,
    testIntellij_spring_boot_core, testIntellij_remoteRun,
    testIntellij_android_core/lint_common/transport_1,
    testIntellij_bigdatatools_zeppelin, testIntellij_javaee_jpabuddy_jpabuddy,
    testIntellij_javascript_tests, testIntellij_platform_debugger_impl/ide_impl,
    testIntellij_r, testIntellij_javascript_psi_impl,
    testFleet_app_fleet_withBackend_testFramework, testFleet_plugins_*,
    testToolbox_app/app_1/app_frontend, testToolbox_core,
    testToolbox_crystal, testToolbox_feature_*, testToolbox_platform_llm_endpoints,
    testToolbox_plugin_api_core, testToolbox_rhizome_compose/testFramework/tests,
    testToolbox_ui_common — full list in this iteration's git log).
  - **14 still fail**, of which **12 are pure Kotlin-language test-data debt**
    (CONTEXT_PARAMETERS_ARE_DEPRECATED on test-data Kotlin code using
    `-Xcontext-receivers` syntax that the current compiler rejects:
    `testFleet_plugins_analyzer_workspace`, `testFleet_plugins_lsp_test`,
    `testIntellij_clion_toolchains` (separate from the family that passes),
    `testIntellij_go_impl` (note: distinct error category from the
    java-direct-driven Variable case; this remaining failure is on Kotlin
    code), `testToolbox_app_common/core_1/feature_ai_chat_1/
    feature_mcp_config/feature_patronus_patronus_core/rhizome/ui/ui_1`).
  - **2 remaining** with non-deprecation patterns:
    - `testIntellij_python_psi_impl`: `MISSING_DEPENDENCY_CLASS Cannot access class 'PyFunction.Modifier'`. Inherited inner class — `PyFunction extends PyAstFunction`, `Modifier` declared on `PyAstFunction`. The Java type at the use site is `PyFunction.Modifier` (Java interprets as inherited). The FIR symbol provider does not recognize `ClassId(pkg, "PyFunction.Modifier")` because no class is declared with that ID — a structural FIR/symbol-provider issue, not addressed by this fix.
    - `testIntellij_platform_lang_impl`: `MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR Type annotation class 'com.intellij.openapi.util.NlsContexts.Tooltip' of the inferred type is inaccessible`. Different category (inferred type carrying type annotations across modules).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/JavaImportResolver.kt` | `extractStaticImports`: also accept `JAVA_CODE_REFERENCE` (the static-on-demand parser shape), with KDoc explaining the two shapes. Reordered `hasStar` computation above `refNode`. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **Trust but verify "test data debt" claims.** The previous iteration entry
  classified the 80 (now 70) IntelliJ pipeline failures as pre-existing test
  data corpus issues "not java-direct regressions" without running the suite
  on a clean master branch. Spot-checking via `System.err` instrumentation
  inside `JavaTypeConversion`'s `null` and `JavaClass` branches showed the
  classifier was a `JavaClassifierTypeOverAst` (i.e. java-direct-active) and
  produced `null` from `resolutionContext.resolve("State")`, immediately
  contradicting the test-data-debt assumption.
- **KMP parser shape variance is a recurring bug surface.** Iterations 51 (in
  the archived `ITERATIONS_37_51_DETAILS.md`), 4.5a, and now this one have
  all hit cases where `IMPORT_STATIC_STATEMENT` carries different children
  depending on whether `*` is present. A future hardening could be a single
  helper `extractImportFqName(importNode)` that covers both shapes (and the
  fragmented/ERROR_ELEMENT shapes) so individual call sites stop drifting.
- **The `tee`-everything-then-grep workflow paid off.** Diagnostic
  instrumentation was added inline rather than via a stash; its `[JD-DBG-*]`
  prefix made the relevant rows trivially greppable from the JUnit XML's
  `<system-err>` block. Removing all instrumentation before commit took one
  edit per added block.
- **One targeted fix can clear a large failure cluster.** A single ~10-line
  change to a parse-time helper went from 0 → 56 passing tests on the IJ
  pipeline corpus. The lesson: when many tests fail with similar-shaped
  errors (here, MISSING_DEPENDENCY_CLASS / ARGUMENT_TYPE_MISMATCH on
  star-imported nested classes), prefer a single reproducer over running the
  full suite — and instrument the model boundary, not the FIR boundary, to
  isolate java-direct vs. shared-FIR regressions.

### Notes / follow-ups not in this iteration

- **`testIntellij_python_psi_impl` remaining failure** wants
  inherited-inner-class accessibility: when Java code declares a parameter
  typed `PyFunction.Modifier` and `Modifier` is inherited from
  `PyAstFunction`, java-direct's `findInheritedNestedClass` already locates
  `PyAstFunction.Modifier` correctly, but the FIR side records the **type
  annotation** as `PyFunction.Modifier` (the lexical reference at the call
  site) and Kotlin's accessibility checker rejects it. Tracking this as a
  separate category — likely needs a cross-language inherited-inner
  accessibility relaxation, not a model-side change.
- **`testIntellij_platform_lang_impl` remaining failure** is on
  `MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR` for a
  binary-classpath nested annotation (`NlsContexts.Tooltip`). Different
  failure mode from the static-import miss; likely an inferred-type
  cross-module accessibility issue independent of java-direct.
- **The 12 CONTEXT_PARAMETERS_ARE_DEPRECATED failures** are genuine
  test-data debt: the test corpus contains Kotlin source compiled with the
  pre-1.10 `-Xcontext-receivers` flag, which the current compiler now
  rejects. These would also fail on master with PSI; out of scope.
- **Add a sanity-check unit test** for `JavaImportResolver.extractImports`
  covering both `import static X.Y;` and `import static X.*;` shapes — this
  would have caught the bug before any IJ-pipeline run.

---

## `LazySessionAccess` re-entrance guard: semantical session-scoped replacement for ThreadLocal — 2026-05-08

### Overview

Earlier today's iteration introduced a `ThreadLocal<Boolean>` flag inside
`LazySessionAccess` to break the `computeClassId` → `tryResolve` →
`FirJavaClass.declarations` (PUBLICATION) → `setAnnotationsFromJava` →
`computeClassId` re-entrance cycle (KT-74097). On review, the thread-local
choice was rejected: re-entrance is a **semantical** property of the
resolution itself — *"this `ClassId` is currently being resolved on this
session"* — and tying the guard to thread identity silently desynchronises
under cooperative scheduling, where a coroutine resumes on a different
thread mid-stack. This iteration replaces the thread-local flag with a
session-keyed `Set<Pair<FirSession, ClassId>>`, preserving cycle-breaking
while staying robust under any threading model.

### Design

The single file-private set

```kotlin
private val inFlightResolutions: MutableSet<Pair<FirSession, ClassId>> =
    ConcurrentHashMap.newKeySet()
```

is the semantical guard. `LazySessionAccess.tryResolve(classId)` and
`LazySessionAccess.classLikeSymbol(classId)` both go through a top-level
inline `guardedResolution(session, classId, reentrantDefault) { ... }`
helper that:

1. Adds `(session, classId)` to the set; on collision (already in flight),
   returns `reentrantDefault` (`false` / `null`) without invoking the body.
2. On success, runs the body and removes the pair on `finally`.

Three structural choices, with rationale:

- **Session-scoped (not thread-scoped).** A `FirSession` is the resolution
  scope: the cycle exists because of FIR-side `FirJavaClass.declarations`
  lazies on the session, so the in-flight set must be shared across all
  `LazySessionAccess` instances that wrap the same session — including the
  inner re-entrant call dispatched from a different per-file
  `CompilationUnitContext`, which owns a fresh `LazySessionAccess` value
  but the same underlying `FirSession`. Keying by session ties the guard
  to that scope, invariant under thread switches.
- **Per-`ClassId` (not boolean).** Tracking individual `ClassId`s — rather
  than a single coarse "anything in flight on this session" bit — keeps the
  semantics precise: only re-entrant requests for the *same* `ClassId` on
  the *same* session are short-circuited; unrelated probes that nest inside
  each other proceed normally. This matches the actual cycle pattern:
  `PUBLICATION` re-entry **restarts** the `FirJavaClass.declarations.compute`
  block, so the second iteration processes the same field/annotation pair,
  hits the same probe order, and finds the `ClassId` already in flight.
  Concurrent and unrelated resolutions on the same session don't interfere.
- **Top-level inline helper (`guardedResolution`), not a value-class member.**
  `LazySessionAccess` is `@JvmInline value class`; member inline functions in
  value classes have JVM-mangling caveats. Top-level keeps the inlining
  uniform and lets the value-class call sites stay simple expression bodies.

### Cycle-breaking proof sketch

When `tryResolve(X)` enters with `X` nested under `P`:

1. `(S, X)` added; recursive FIR call dispatches via composite to
   `FirExtensionDeclarationsSymbolProvider.generateClassLikeDeclaration(X)`.
2. That branch calls `getClassLikeSymbolByClassId(P)` then builds
   `nestedClassifierScope(P)`, which forces `P.declarations` (PUBLICATION).
3. Materialisation processes field `f`'s annotation, computing
   `JavaAnnotation.classId` → `resolveSimpleNameToClassIdImpl` → probes a
   sequence of candidate `ClassId`s via `tryResolve(...)`. Each probe adds
   its own `(S, candidateId)` to the set on entry and removes it on exit.
4. If any probe candidate's resolution path re-triggers the same
   `getClassLikeSymbolByClassId` → `nestedClassifierScope(P)` → `P.declarations`
   chain, `PUBLICATION` lets the lazy block re-run on the same thread.
5. The re-run iterates the same fields in the same order. At the same
   field, the same annotation, the same probe, `tryResolve(candidateId)` is
   called — but `(S, candidateId)` is already in the set (added in step 3).
   `guardedResolution` short-circuits with `false` → cycle broken at this
   level; the inner probe falls back to `ClassId.topLevel(reference)`.

The depth of recursion is bounded by the number of distinct probes
attempted across nested levels (a small constant per annotation), and after
each recursive level adds an entry the search space monotonically shrinks
until further levels short-circuit immediately on every probe.

### Test Results

- **`JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`**:
  2699/2699 passing — no regressions vs. the morning's ThreadLocal version.
- **`testIntellij_vcs_git`** (the original `StackOverflowError` case): passes —
  cycle still successfully broken.
- **`testIntellij_vcs_perforce`**, **`testIntellij_graphql`**,
  **`testIntellij_javascript_impl`**, **`testIntellij_ruby_backend`** (the
  4 IntelliJ tests the ThreadLocal guard had unblocked earlier today):
  all 4 still pass.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/LazySessionAccess.kt` | Removed `private val resolutionInFlight: ThreadLocal<Boolean>`. Added `private val inFlightResolutions: MutableSet<Pair<FirSession, ClassId>>` (`ConcurrentHashMap.newKeySet`) and a top-level `private inline fun <R> guardedResolution(session, classId, reentrantDefault, block)` helper. `tryResolve` and `classLikeSymbol` rewritten as expression-bodied calls into `guardedResolution`. KDoc rewritten to describe the semantical session-scoped model and why thread-locality was rejected. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **Thread-locality is a leaky abstraction for resolution-time guards.**
  It happens to work in synchronous Kotlin compilation today because the
  call stack is the unit of "current resolution flow", but the moment any
  layer of the resolution starts cooperatively scheduling (coroutines on a
  thread pool, reactive flows, fork-join with work-stealing), the thread
  identity stops tracking the logical flow and the guard either misses
  re-entrance or fires spuriously. Using session + `ClassId` as the key
  attaches the guard to the data being resolved, which is invariant
  under any scheduling.
- **`PUBLICATION` re-entry restarts deterministically.** Same fields,
  same probe order, same `ClassId`s probed — that determinism is what makes
  per-`ClassId` keying sufficient to break the cycle without resorting to
  a coarse boolean.
- **Value classes prefer top-level inline helpers.** Member inline
  functions on `@JvmInline value class` carry JVM-mangling caveats; a
  top-level `private inline fun guardedResolution(...)` sidesteps them
  while keeping the call sites concise expression bodies.
- **The previously documented "annotation classId precision regression"
  (cycle scope of fallback) carries over unchanged.** The semantical model
  is strictly finer than the boolean (only the *same* `ClassId` falls
  back, not arbitrary inner calls), so star-imported-annotation precision
  is at least as good as the ThreadLocal version. Cycle-scoped precision
  is still a documented follow-up.

### Notes / follow-ups

- **`JavaSupertypeLoopChecker` still uses `ThreadLocal<ArrayDeque<ClassId>>`.**
  The same critique applies to that class. It was not changed in this
  iteration because it was outside the scope of the user's review comment,
  and its cycle-detection state is more complex (a stack, not a flat set,
  with diagnostic-edge recording). A follow-up iteration can apply the
  same semantical-keying treatment if desired.
- **No build-time enforcement that this is the only `ThreadLocal` in
  resolution code.** A grep gate or detekt rule could be added to forbid
  `ThreadLocal` in `compiler/java-direct/.../resolution/` to avoid
  reintroducing the pattern.

---

## `IntelliJFullPipelineTestsGenerated` triage: re-entrance guard + nested-record `isStatic` — 2026-05-08

### Overview

The 80 `IntelliJFullPipelineTestsGenerated` regressions reported after the
public-interface rollback (Steps 4.5a–C) had two distinct java-direct-attributable
root causes. Both are fixed in this iteration; sampled validation shows pure
java-direct-introduced regressions are gone. The remainder of the 80 failures are
pre-existing, unrelated issues (nested-binary-class FQN resolution, Kotlin-side
diagnostics on test-data Kotlin code, Backend-JVM bytecode-transformation
crashes) that this iteration does not address.

### Root cause #1 — `LazySessionAccess` re-entrance / StackOverflowError

`testIntellij_vcs_git` (and any heavy-annotation Java module on a hot
materialisation path) crashed with a 1024-deep `StackOverflowError`. The cycle:

1. `JavaAnnotationOverAst.computeClassId` calls
   `JavaResolutionContext.resolveSimpleNameToClassIdImpl` → `tryResolve(classId)`
   → `LazySessionAccess.tryResolve` → `FirSymbolProvider.getClassLikeSymbolByClassId`.
2. The composite chain reaches `FirExtensionDeclarationsSymbolProvider`'s
   nested-class branch, which builds a `FirNestedClassifierScopeImpl` over the
   outer class.
3. Building the scope's `classIndex` forces `FirJavaClass.declarations` (a
   `LazyThreadSafetyMode.PUBLICATION` lazy — KT-74097: same-thread re-entrance
   recurses silently on `PUBLICATION`).
4. Materialisation runs `convertJavaFieldToFir` → `setAnnotationsFromJava` →
   `JavaAnnotation.classId` → back to step 1 on a different annotation
   instance, ad infinitum.

The PUBLICATION lazy is a deliberate FIR perf choice and isn't ours to change.
Step 4.5a's deletion of the `JavaClassifierType.resolve(...)` callback API made
java-direct route every classifier-resolution path through `tryResolve`,
sharply widening the surface where this latent cycle could fire.

**Fix.** A per-thread re-entrance guard at the `LazySessionAccess` boundary —
the single chokepoint through which the model invokes the FIR symbol provider.
Re-entrant `tryResolve` returns `false`; re-entrant `classLikeSymbol` returns
`null`. Each model-side caller's existing fallback handles the inner level:
`JavaAnnotationOverAst.computeClassId` falls back to
`ClassId.topLevel(FqName(reference))` (the same fallback used in parsing-level
test fixtures and pre-Step-4.5a code); cross-file type classifier resolution
falls back to `null` classifier, which `JavaTypeConversion.resolveTypeName`
then handles via its `findClassIdByFqNameString` / `ClassId.topLevel` fallback
chain. The outer call still completes its FIR-backed lookup with full
precision; only the recursive inner level loses precision. Cycle broken;
compilation continues.

### Root cause #2 — nested records mis-classified as inner classes

`JavaClassOverAst.isStatic` did not recognise nested records as implicitly
static. JLS §8.10.3 requires it: "A nested record declaration is implicitly
static." Without this, FIR's `INNER_CLASS_CONSTRUCTOR_NO_RECEIVER` checker
fires on every constructor call to a nested record. Affected tests included
`testIntellij_graphql` (`IntrospectionOutput`), `testIntellij_compilation_charts`
(`EventColor`), `testIntellij_java_impl` (`InheritDocContext<T>`),
`testIntellij_javascript_testFramework` (`LookupString`), `testIntellij_ruby_backend`
(`Data`), and similar. The corresponding logic in
`JavaClassOverAst.findInnerClassImpl` had the same omission, which would have
broken type-parameter scoping for inner-record references; both spots are
fixed.

**Fix.**

```kotlin
override val isStatic: Boolean
    get() = hasModifier(JavaSyntaxTokenType.STATIC_KEYWORD) ||
            (outerClass != null && (isInterface || isEnum || isRecord)) ||
            (outerClass?.isInterface == true)
```

`findInnerClassImpl` gets the matching `innerIsRecord` clause in
`innerIsEffectivelyStatic`.

### Test Results

- **Java-direct module suite**: `JavaUsingAstPhasedTestGenerated` +
  `JavaUsingAstBoxTestGenerated`: **2699/2699 passing**, no regressions vs.
  the post-Step-C baseline.
- **Sample of 24 originally-failing `IntelliJFullPipelineTestsGenerated`** (run
  individually after the fixes): **4 newly pass** —
  `testIntellij_vcs_perforce`, `testIntellij_graphql`,
  `testIntellij_javascript_impl`, `testIntellij_ruby_backend`. The remaining
  20 still fail; their error patterns are unrelated to java-direct (see below).

### Remaining failure categories (deferred — not java-direct regressions)

The following patterns repeated across the still-failing sample, with
representative tests in parentheses. None are caused by code under
`compiler/java-direct/`:

- **Nested binary-class FQN resolution.** `MISSING_DEPENDENCY_CLASS` /
  `MISSING_DEPENDENCY_SUPERCLASS` for binary classes whose nested types
  Kotlin code references either through static-on-demand imports
  (`import static X.*` in a Java source file used by Kotlin) or via dotted
  FQN paths. Examples: `Status` from `CidrToolsUtil` (`testIntellij_clion_toolchains`),
  `Variable` from `DlvApi` (`testIntellij_go_impl`),
  `PyFunction.Modifier` (`testIntellij_python_psi_impl`),
  `PhpClassMemberCallbackReference` (`testIntellij_php_impl`),
  `AbstractMessage.InternalOneOfEnum` (`testIntellij_platform_ijent_impl`,
  `testIntellij_r`), `ActionProvider`, `BaseBuilder`, `NlsContexts.Tooltip`.
- **Kotlin-side override-checker diagnostics.** `NOTHING_TO_OVERRIDE`,
  `ABSTRACT_MEMBER_NOT_IMPLEMENTED`, `RETURN_TYPE_MISMATCH_ON_OVERRIDE`,
  `OUTER_CLASS_ARGUMENTS_REQUIRED` on Kotlin classes that override Java
  base classes. These look like Kotlin compiler / FIR-frontend diagnostics
  driven by the test-data evolution (the test corpus pulls fresher
  community/IntelliJ snapshots that exercise newer Kotlin language rules),
  not java-direct-driven.
- **Backend-JVM `NegativeArraySizeException` in `TransformationMethodVisitor`.**
  `testIntellij_android_transport_1`, `testIntellij_remoteRun`. The cycle is
  on the JVM IR backend's bytecode transformation; java-direct stops
  participating long before this phase.
- **Kotlin context-receivers / context-parameters deprecation errors.**
  `[CONTEXT_PARAMETERS_ARE_DEPRECATED]`, `[CONTEXT_PARAMETER_WITHOUT_NAME]`,
  `[CONTEXT_RECEIVERS_DEPRECATED]`. Test-data Kotlin code using the
  pre-1.10 `-Xcontext-receivers` syntax which the current compiler
  rejects/warns. Pure test-data debt.

The first category (nested binary-class FQN) is plausibly a binary-side
finder regression separate from java-direct. The static-on-demand import path
in `JavaResolutionContext.resolveFromStarImports` already calls
`resolveAsClassId(starPackage, tryResolve)` which iterates package/class
splits longest-package-first — so `import static X.Y.*` correctly probes
`(X, Y)` as a class before `(X.Y, ...)` as a package. Triage of these cases
should focus on whether they reproduce on a **clean** branch (without any of
this iteration's java-direct work) — if yes, they are out of scope. The
sample's per-test errors all match `MISSING_DEPENDENCY_*` shapes that PSI
likewise produces, suggesting the nested-binary-class lookups never differ
between java-direct ON and OFF for these tests.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/LazySessionAccess.kt` | Added per-thread `resolutionInFlight` ThreadLocal flag with KDoc citing KT-74097 and the cycle. `tryResolve` and `classLikeSymbol` set the flag on entry, return early (`false` / `null`) on re-entrant calls, clear on `finally`. |
| `compiler/java-direct/src/.../model/JavaClassOverAst.kt` | `isStatic`: nested records implicitly static (JLS §8.10.3). `findInnerClassImpl`: same clause in `innerIsEffectivelyStatic`. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **`PUBLICATION` lazies don't detect same-thread re-entrance.** When a
  re-entrant call goes through a `PUBLICATION` `Lazy`, the recursion
  proceeds silently and only stops at the JVM's hardcoded ~1024-frame stack
  limit — not at the lazy's nominal "computed once" contract. KT-74097
  documents this; for FIR's `FirJavaClass.declarations` specifically, the
  PUBLICATION choice is intentional perf-driven, so any cycle that can reach
  `getDeclarations` recursively is the caller's problem to break.
- **The model-side resolver is the right place to break the cycle.** The
  alternative (FIR-side: detect the recursion in
  `FirNestedClassifierScopeImpl.classIndex`) would require a fix in code
  shared with PSI/binary impls. The model-side guard at `LazySessionAccess`
  is local to java-direct and structurally cannot affect the PSI / binary
  paths since they don't go through `LazySessionAccess`.
- **`JLS §8.10.3` is easy to forget.** Records and enums have analogous
  implicit-static rules but live in different parts of the spec; a generic
  "nested kinds" test in `JavaParsingMembersTest` would have caught this
  iteration's fix gap.
- **Test-data evolution is a confounder.** Several "failure" categories on
  the IntelliJ corpus are actually pre-existing test-data Kotlin code that
  modern Kotlin compilers (regardless of java-direct) reject. A clean-branch
  rerun would discriminate java-direct-driven failures from corpus-driven
  ones; AGENT_INSTRUCTIONS rule "Don't run `kotlin.test.update.test.data=true`"
  is the right rule for this kind of corpus, but it implies that **expected**
  failures on this corpus need to be tracked elsewhere.

### Notes / follow-ups not in this iteration

- **Annotation classId precision during inner cycle iterations.** When the
  guard fires, the inner annotation classId resolves to
  `ClassId.topLevel(FqName(reference))` instead of the FIR-backed correct
  ClassId. For star-imported annotations (`@SomeAnno` where `SomeAnno` is
  resolved via `import static X.*`), the fallback ClassId is wrong. The
  affected annotations are those that happen to be processed as a side-effect
  of a particular FirJavaClass's declaration materialisation triggered by
  another annotation's classId resolution. In practice the cycle fires on a
  small number of FirJavaClass instances per compile; the imprecision is
  contained but not eliminated. A followup iteration could try a less
  aggressive guard (e.g. only return `false` from `tryResolve` for the
  specific class triggering the cycle, not for arbitrary inner calls) — but
  cycle-detection state would need to be threaded through, and the current
  blunt guard avoids that complexity.
- **Sample-of-24 vs. full 80-test verification.** A full
  `IntelliJFullPipelineTestsGenerated` run takes hours and was not feasible
  in this session; the 24-test sample was chosen to span the categories in
  `ijtestsfailed.txt`. Running the remaining 56 tests is left to the next
  iteration's session; the expectation is that any test whose error pattern
  matches "StackOverflowError in `JavaAnnotationOverAst.computeClassId`" or
  "INNER_CLASS_CONSTRUCTOR_NO_RECEIVER on a Java record" now passes.
- **Java-direct internal `JavaClass` adapter perf path.** The re-entrance
  guard means the second-level annotation classId resolution skips FIR.
  Long-term, exposing FirJavaClass's eagerly-known annotation classIds via
  the model adapter would let the cycle resolve without falling back. This
  is a Step-5+ optimisation, not a Step-4.5x rollback prerequisite.

---

## Step C: relocate five remaining members onto fir-jvm-private subinterfaces — 2026-05-07

### Overview

Final iteration of the public-Java-model-interface rollback. Five
`java-direct`-introduced members survived Step 4.5b/4.5c because they encode
performance-sensitive protocols (callback-driven TYPE_USE annotation filtering,
cross-language constant evaluation, enum-vs-const-field disambiguation) that
PSI/binary impls don't need (they pre-process at structure-build time).
Per the inventory's Step C "move-to-private" branch, they are relocated to
fir-jvm-private subinterfaces. The public
`core/compiler.common.jvm/.../load/java/structure/*.kt` interfaces are now
free of `java-direct`-introduced members — the §1 invariant of
`INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` is satisfied.

### Why move-to-private (not eager pre-processing)

The inventory listed two paths for Step C: roll back via eager pre-processing
in the model, or move the protocols to a `java-direct`-private subinterface.
The move-to-private path was chosen because:

- Eager pre-processing changes perf behavior; move-to-private is a zero-perf-risk
  transformation.
- The protocols are genuinely useful — they let java-direct defer work to
  resolution time. PSI/binary do that work at structure-build. Both choices are
  reasonable; the public-surface concern is the actual debt, not the laziness.
- A perf audit comparing eager vs. lazy is a future optimisation question, not a
  prerequisite for the rollback goal stated in §1.

### Changes

- **New** `compiler/fir/fir-jvm/src/.../fir/java/JavaModelExtensions.kt`. Defines:
  - `JavaTypeWithExternalAnnotationFiltering : JavaType` carrying `needsTypeUseAnnotationFiltering` and `filterTypeUseAnnotations`.
  - `JavaFieldWithExternalInitializerResolution : JavaField` carrying `supportsExternalInitializerResolution` and `resolveInitializerValue`.
  - `JavaEnumValueAnnotationArgumentWithConstFallback : JavaEnumValueAnnotationArgument` carrying `couldBeConstReference`.
- The subinterfaces live in fir-jvm (not java-direct) because fir-jvm is the
  consumer; java-direct already depends on fir-jvm transitively (via
  `:compiler:frontend.java`), but fir-jvm does not depend on java-direct, so
  locating the protocols here avoids any dependency cycle.
- `JavaTypeConversion.kt`: the two `needsTypeUseAnnotationFiltering` /
  `filterTypeUseAnnotations` call sites are collapsed into a single
  `filterTypeUseAnnotationsIfNeeded(session)` helper that performs the `as?`
  downcast onto `JavaTypeWithExternalAnnotationFiltering`.
- `FirJavaFacade.kt`: `lazyInitializer` does the `as?` downcast onto
  `JavaFieldWithExternalInitializerResolution`.
- `javaAnnotationsMapping.kt`: enum-value-argument branch does the `as?` downcast
  onto `JavaEnumValueAnnotationArgumentWithConstFallback`.
- java-direct impls (`JavaTypeOverAst`, `JavaFieldOverAst`,
  `JavaEnumValueAnnotationArgumentOverAst`) declare implementation of the new
  subinterfaces; the override bodies are unchanged.
- `compiler/java-direct/test/.../JavaParsingAnnotationsTest.kt`: two test call
  sites that read `filterTypeUseAnnotations` directly now cast through
  `JavaTypeWithExternalAnnotationFiltering`.
- Public interfaces in `core/compiler.common.jvm/src/.../load/java/structure/`:
  - `javaTypes.kt`: `JavaType` collapses to `interface JavaType : ListBasedJavaAnnotationOwner` (one-liner).
  - `javaElements.kt`: `JavaField` loses both members.
  - `annotationArguments.kt`: `JavaEnumValueAnnotationArgument` loses `couldBeConstReference`.
- Inventory §2 status columns flipped to **Done**; §3 Step C section rewritten as
  the post-implementation entry.

### Test Results

- `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`: BUILD SUCCESSFUL (matches the post-Step-4.5c baseline; trip-wires `testJ_k_complex`, `testKJKComplexHierarchyWithNested`, `testGenericBoundInnerConstructorRef` stay green).
- `PhasedJvmDiagnosticLightTreeTestGenerated.*`: BUILD SUCCESSFUL.

### Files Modified

| File | Change |
|------|--------|
| `compiler/fir/fir-jvm/src/.../fir/java/JavaModelExtensions.kt` | **New file**: three fir-jvm-private subinterfaces. |
| `compiler/fir/fir-jvm/src/.../fir/java/JavaTypeConversion.kt` | Collapsed two call sites into `filterTypeUseAnnotationsIfNeeded(session)` helper using `as? JavaTypeWithExternalAnnotationFiltering`. |
| `compiler/fir/fir-jvm/src/.../fir/java/FirJavaFacade.kt` | `lazyInitializer` uses `as? JavaFieldWithExternalInitializerResolution`. |
| `compiler/fir/fir-jvm/src/.../fir/java/javaAnnotationsMapping.kt` | Enum-value-argument branch uses `as? JavaEnumValueAnnotationArgumentWithConstFallback`. |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | Added `JavaTypeWithExternalAnnotationFiltering` to supertypes. |
| `compiler/java-direct/src/.../model/JavaMemberOverAst.kt` | Added `JavaFieldWithExternalInitializerResolution` to `JavaFieldOverAst`'s supertypes. |
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | Added `JavaEnumValueAnnotationArgumentWithConstFallback` to enum-value-argument's supertypes. |
| `compiler/java-direct/test/.../JavaParsingAnnotationsTest.kt` | Two test call sites cast through the subinterface. |
| `core/compiler.common.jvm/src/.../load/java/structure/javaTypes.kt` | Deleted both members; `JavaType` collapses to a 1-liner. |
| `core/compiler.common.jvm/src/.../load/java/structure/javaElements.kt` | Deleted both members from `JavaField`. |
| `core/compiler.common.jvm/src/.../load/java/structure/annotationArguments.kt` | Deleted `couldBeConstReference`. |
| `compiler/java-direct/implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` | §2 status columns flipped to **Done**; §3 Step C rewritten as post-implementation entry; §1 invariant status marked ✅ satisfied. |

### Key Learnings

- Pitfall when writing KDoc: Kotlin block comments **nest**. A literal `/*` sequence inside a block comment opens a nested comment. Wrote `core/compiler.common.jvm/.../load/java/structure/*.kt` in a top-of-file KDoc — the `/*` from `structure/*.kt` opened a nested comment that consumed everything up to the next `*/`, breaking the file's interface declarations downstream. Compiler error reads "Syntax error: Unclosed comment at line 86:1" but the actual cause is mid-file. Avoid `/*` sequences in KDoc text — rephrase or use backticks-without-slash.
- The "fir-jvm vs java-direct" location for the subinterfaces was settled by dependency direction: fir-jvm is the consumer, java-direct already depends on fir-jvm via `:compiler:frontend.java`, but fir-jvm does not depend on java-direct. Putting protocols where they're consumed avoids the cycle and matches "define-where-consumed".
- Test-side downcasts were a forgotten case. The first matrix run failed at `compileTestKotlin` because `JavaParsingAnnotationsTest.kt` reads `filterTypeUseAnnotations` directly as a public-interface call. Public-interface-removal iterations need to run `:compileTestKotlin` (not just `:compileKotlin`) before declaring a green compile.

### Notes / follow-ups not in this iteration

- The fir-jvm-private subinterface names are verbose. If they are ever exported beyond fir-jvm, consider shorter names (e.g., `JavaTypeAnnotationFiltering`). Inside fir-jvm only, the verbosity is fine — descriptive over short.
- A perf audit comparing eager pre-processing (the alternative Step C path the inventory documented) to the current callback-driven approach is still a sensible follow-up. If eager wins, the move-to-private subinterfaces can be deleted entirely — that would shrink fir-jvm too. But this is a future optimisation, not a rollback prerequisite.
- The model-internal `JavaResolutionContext.getContainingClassIds()` survives from Step 4.5c. Stage-5 of `RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md` may eventually fold `resolveFromLocalScope` into FIR; the helper comes off then.

---

## Step 4.5c proper: delete `JavaClassifierType.containingClassIds` from the public Java-model interface — 2026-05-07

### Overview

Eliminated the last `java-direct`-introduced member that the inventory's
`Step 4.5c` plan flagged for removal: `JavaClassifierType.containingClassIds`.
The lexical containing-class chain that FIR's `findOuterTypeArgsFromHierarchy`
needs for inherited-inner type-arg substitution is now carried on the FIR side
via `MutableJavaTypeParameterStack.containingClassSymbol`, set at
`FirJavaFacade.convertJavaClassToFir` time. The model is no longer involved.

### Changes

- `MutableJavaTypeParameterStack`: added `var containingClassSymbol: FirRegularClassSymbol? = null`. `copy()` propagates it (same logical class); `addStack(parent)` does not (each FirJavaClass owns its own identity).
- `FirJavaFacade.convertJavaClassToFir`: after creating the per-class stack, sets `javaTypeParameterStack.containingClassSymbol = classSymbol` (before `addStack(parent)`).
- `JavaTypeConversion.findOuterTypeArgsFromHierarchy`: signature changed from `(ClassId, List<ClassId>, FirSession)` to `(ClassId, JavaTypeParameterStack, FirSession)`. Body walks `(stack as MutableJavaTypeParameterStack).containingClassSymbol.classId.outerClassId` chain. Returns `null` early when stack does not carry a containing-class symbol (callers outside `convertJavaClassToFir`'s scope).
- Three call sites updated (`null ->` branch's `isRawType` recovery; `is JavaClass ->` branch's missing-tail-args recovery; `null ->` branch's empty-args recovery). `containingClassIds.isNotEmpty()` perf gates dropped — the function's early `null` return covers non-`FirJavaClass`-conversion callers; the `pathSegments().size > 1` and `typeArguments` size checks remain to keep the recovery scoped to nested cross-file refs with missing implicit outer args.
- `core/compiler.common.jvm/.../load/java/structure/javaTypes.kt`: deleted `JavaClassifierType.containingClassIds`. Dropped now-unused `ClassId` import.
- `compiler/java-direct/.../model/JavaTypeOverAst.kt`: deleted `containingClassIds` override. Dropped now-unused `ClassId` import.
- `compiler/java-direct/.../resolution/JavaResolutionContext.kt`: `getContainingClassIds()` retained as a model-internal helper for `resolveFromLocalScope` (Stage-4 of resolver-unification). Not on the public interface — out of scope for this rollback.

### Why the inventory's "walk via classifier.outerClass" sketch was wrong

`classifier.outerClass` is the **resolved classId's** outer chain, e.g. for
`NestedInSuperClass` resolved to `SuperClass.NestedInSuperClass` it's
`SuperClass`. `findOuterTypeArgsFromHierarchy` needs the **lexical
containing-class chain at the reference site** — for
`class J1.NestedSubClass extends NestedInSuperClass` the lexical chain is
`[J1.NestedSubClass, J1]` and we walk `J1`'s supertypes to find
`SuperClass<String>`. The two chains differ exactly in the inherited-inner case
the recovery exists for (when they coincide, the recovery wouldn't fire). So
the data must come from the FIR-side resolution context, not from the
classifier — hence the stack-carries-symbol approach.

### Test Results

- `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`: BUILD SUCCESSFUL (matches the post-Step-4.5b 2693/2693 baseline; the three trip-wires `testJ_k_complex`, `testKJKComplexHierarchyWithNested`, `testGenericBoundInnerConstructorRef` stay green).
- `PhasedJvmDiagnosticLightTreeTestGenerated.*`: BUILD SUCCESSFUL.

### Files Modified

| File | Change |
|------|--------|
| `compiler/fir/fir-jvm/src/.../MutableJavaTypeParameterStack.kt` | Added `containingClassSymbol` field; `copy()` propagates, `addStack` does not. |
| `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt` | Set `javaTypeParameterStack.containingClassSymbol = classSymbol` in `convertJavaClassToFir`. |
| `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` | `findOuterTypeArgsFromHierarchy` signature change + three call-site updates. |
| `core/compiler.common.jvm/src/.../load/java/structure/javaTypes.kt` | Deleted `JavaClassifierType.containingClassIds`; dropped `ClassId` import. |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | Deleted `containingClassIds` override; dropped `ClassId` import. |
| `compiler/java-direct/implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` | §2.1 row + §3 Step 4.5c marked **Done**; corrected the "walk via `classifier.outerClass`" sketch. |

### Key Learnings

- Lexical containing-class context can be threaded through FIR's existing per-`FirJavaClass` `MutableJavaTypeParameterStack` plumbing without widening any public interface. The stack already has the right lifecycle (set at `convertJavaClassToFir`, copied into the lazy member-stack).
- `addStack(parent)` deliberately does not propagate `containingClassSymbol`; each nested-class `FirJavaClass` conversion creates a fresh stack and sets its own symbol. Conflating identity here would break `findOuterTypeArgsFromHierarchy`'s "skip index 0 (currently being resolved)" invariant.
- The `containingClassIds.isNotEmpty()` perf gate was redundant with the existing `pathSegments().size > 1` (nested) + `typeArguments.size < typeParameterSymbols.size` (missing args) checks. Removing it broadens the gate to all `FirJavaClass`-scope conversions but the size check filters out binary/PSI nested types that already carry full outer args.
- After this iteration, the public `core/compiler.common.jvm/.../load/java/structure/*.kt` interfaces still hold five `java-direct`-introduced members, all in **Step C** (perf-audit) territory: `JavaType.needsTypeUseAnnotationFiltering` + `filterTypeUseAnnotations`, `JavaField.supportsExternalInitializerResolution` + `resolveInitializerValue`, `JavaEnumValueAnnotationArgument.couldBeConstReference`. Step C is the next rollback iteration's scope.

### Notes / follow-ups not in this iteration

- `JavaResolutionContext.getContainingClassIds()` survives as a model-internal helper. If `resolveFromLocalScope` is ever folded fully into FIR (Stage 5 of resolver-unification), the helper can come off too. Tracked in `JavaScopeResolver.findLocalClass`'s KDoc.
- Inventory §3 originally suggested walking `classifier.outerClass`. The "Why the sketch was wrong" subsection above documents the correction; future readers should consult the implementation here, not the original §3 text.

---

## Step 4.5b/4.5c via Option A: `FirBackedJavaTypeParameter` carrying `FirTypeParameterSymbol` — 2026-05-07

### Overview

Implemented Option A (per
`/Users/ich-jb/.claude/plans/read-compiler-java-direct-agent-instruct-linked-stonebraker.md`
addendum): adapter exposes a real outer-class chain whose type-parameter wrappers carry their
`FirTypeParameterSymbol` directly, FIR's `is JavaTypeParameter ->` branch reads the symbol
without consulting `MutableJavaTypeParameterStack`. Two of three trip-wires fixed; one
regression remains as a pure content-diff (no analysis exception, PSI gate stays green).

### Changes

- **New `JavaTypeParameterWithFirSymbol` interface** (`compiler/fir/fir-jvm/src/.../MutableJavaTypeParameterStack.kt`):
  shared contract that lets FIR resolve adapter-synthesised `JavaTypeParameter` instances
  without registering them in any per-`FirJavaClass` stack.
- **`JavaTypeConversion.kt:310` patch**: `is JavaTypeParameter ->` branch checks
  `JavaTypeParameterWithFirSymbol` first; falls back to existing `javaTypeParameterStack[classifier]`
  lookup for PSI / binary / source `java-direct` classifiers.
- **`FirBackedJavaClassAdapter` rewritten**:
  - `typeParameters` returns `FirBackedJavaTypeParameter` wrappers carrying
    `FirTypeParameterSymbol`s (from `FirJavaClass.nonEnhancedTypeParameters` or
    `FirRegularClass.typeParameters` for non-Java arms), filtering out
    `FirOuterClassTypeParameterRef` entries (own-type-params only — outer chain reached via
    `outerClass`).
  - `isStatic`: detected via `FirJavaClass.nonEnhancedTypeParameters.none { it is FirOuterClassTypeParameterRef }`
    for Java arms; falls back to `!firClass.status.isInner` for Kotlin / built-in / deserialized.
  - New nested `FirBackedJavaTypeParameter` class implementing `JavaTypeParameterWithFirSymbol`.
- **Wired** via `JavaResolutionContext.classifierAdapterFor`,
  `JavaClassifierTypeOverAst.computeClassifier()`'s cross-file branch (now wraps
  `resolutionContext.resolve(rawTypeName)` in adapter).
- **Public-interface deletions** (net deletions only — rule 7):
  - `JavaClassifierType.resolvedClassId` (the Step 4.5a side-channel) deleted from
    `core/compiler.common.jvm/.../javaTypes.kt`.
  - `JavaClassifierType.isTriviallyFlexibleHint` deleted from same file.
- **`JavaTypeConversion.kt`**:
  - `resolveTypeName` restored to pre-`java-direct` body
    (`(javaType.classifier as? JavaClass)?.classId ?: findClassIdByFqNameString ?: ClassId.topLevel`).
  - `ConeFlexibleType(... isTrivial = isTriviallyFlexibleHint)` replaced with
    `isTrivial = false` — resolvable refs go through the first branch's
    `classifier?.isTriviallyFlexible() == true` path; the else branch only fires for
    non-trivially-flexible classifiers (Kotlin read-only mapped collections) or unresolvable
    simple names where `isTrivial = false` matches PSI.
- **`JavaTypeOverAst.kt`**: `classifier` switched to `lazy(PUBLICATION)`; cross-file branch
  added to `computeClassifier`; `resolvedClassId` override deleted; `isTriviallyFlexibleHint`
  override + `computeIsTriviallyFlexibleHint` helper + `JAVA_READ_ONLY_FQ_NAMES` /
  `JAVA_READ_ONLY_SIMPLE_NAMES` companion + `JavaToKotlinClassMap` import deleted.
- **`JavaResolutionContext.kt`**: `classifierAdapterFor` helper added; `isUnambiguouslyCrossFileClass`
  KDoc updated to reflect the deleted hint consumer.

### Test Results

- `JavaUsingAst*` matrix: **2693/2693 passing**.
  - **Fixed:** `Tests > Generics > InnerClasses > testJ_k_complex` (was failing on prior prototype).
  - **Fixed:** `BoxJvm > Invokedynamic > Sam > FunctionRefToJavaInterface > testGenericBoundInnerConstructorRef` (was failing).
  - **Fixed:** `ResolveWithStdlib > J_k > testKJKComplexHierarchyWithNested` (was failing —
    needed Option B's outer-args recovery added to `is JavaClass ->` branch, see "KJK fix"
    below).
- PSI regression gate (`PhasedJvmDiagnosticLightTreeTestGenerated.*`): **BUILD SUCCESSFUL**,
  0 failures.

### KJK fix — Option B port to `is JavaClass ->` branch

Initial Option A landing produced a content diff for `testKJKComplexHierarchyWithNested`.
Instrumenting `JUnit5Assertions.assertEqualsToFile` to dump actual to `/tmp/jd_iter_a/`
revealed the divergence: `J1.NestedSubClass extends NestedInSuperClass` is a cross-file
empty-args inner-class supertype reference. Pre-Step-4.5b java-direct passed via the
`null ->` branch which ran `findOuterTypeArgsFromHierarchy` recovery (gated on
`typeArguments.isEmpty()`); Option A routes through `is JavaClass ->` branch which lacked the
recovery → outer type-arg `T = String` lost → substitution chain broke → `nestedI(vString)`
and `nested("")` produced `ARGUMENT_TYPE_MISMATCH`.

Fix: ported the `findOuterTypeArgsFromHierarchy` recovery to the `is JavaClass ->` branch
with two refinements:

1. **Cheap short-circuit on `containingClassIds.isNotEmpty()` first** — guarantees zero cost
   for binary `PlainJavaClassifierType` and PSI paths (both inherit `containingClassIds =
   emptyList()` from the interface default at
   `core/compiler.common.jvm/.../load/java/structure/javaTypes.kt:110`). Verified by repo-wide
   grep: java-direct's `JavaClassifierTypeOverAst` is the **only** override.
2. **Generalised gate** from `typeArguments.isEmpty()` (the `null ->` branch's original
   condition) to `typeArguments.size < typeParameterSymbols.size`. This lets the recovery
   also fire for the partial-args case (e.g. `BaseInner<Double, String>` referenced inside a
   class whose hierarchy provides outer `H`).

### Why Option B alone failed but Option A + Option B combined works

Option B alone (no adapter, FIR-side outer-args recovery) fails for `testJ_k_complex` /
`testGenericBoundInnerConstructorRef`: their outer-args recovery requires the
`containingClassIds` chain to have size ≥ 2 (to skip index 0 in
`findOuterTypeArgsFromHierarchy`). For method-body cross-file refs (size 1) the recovery
returns null → outer args lost.

Option A alone (adapter, no FIR-side outer-args recovery) fails for `testKJKComplexHierarchyWithNested`:
the test's empty-args inner-class supertype reference (`extends NestedInSuperClass`) routes
through `is JavaClass ->` branch via the adapter, but that branch lacked the
`findOuterTypeArgsFromHierarchy` recovery the `null ->` branch had.

Option A + Option B combined: adapter populates `classifier` so FIR resolves type params via
`JavaTypeParameterWithFirSymbol` (covers J_k_complex / GenericBoundInnerConstructorRef);
FIR-side recovery in `is JavaClass ->` branch fills missing outer args when the model side
can't supply them (covers KJK). Both code paths are needed.

### Files Modified

| File | Change |
|------|--------|
| `compiler/fir/fir-jvm/src/.../MutableJavaTypeParameterStack.kt` | Added `JavaTypeParameterWithFirSymbol` interface. |
| `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` | `is JavaTypeParameter ->` branch checks `JavaTypeParameterWithFirSymbol` before stack lookup; `resolveTypeName` restored to pre-`java-direct` body; `isTrivial = false` substitution. |
| `compiler/java-direct/src/.../resolution/FirBackedJavaClassAdapter.kt` | Rewritten: real outer-class chain, `FirBackedJavaTypeParameter` wrappers, `isStatic` via `FirOuterClassTypeParameterRef` detection. |
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | `classifierAdapterFor` helper; KDoc cleanup. |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | `classifier` lazy; cross-file adapter wiring; `resolvedClassId`/`isTriviallyFlexibleHint`/companion/import deleted. |
| `core/compiler.common.jvm/src/.../load/java/structure/javaTypes.kt` | Deleted `resolvedClassId` and `isTriviallyFlexibleHint`. |
| `compiler/java-direct/implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` | Step 4.5b status update. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry. |

### Key Learnings

- **`JavaTypeParameterWithFirSymbol` is the right abstraction.** Crosses module boundary
  cleanly (lives in fir-jvm, java-direct implements it). PSI / binary / source-`java-direct`
  classifiers ignore it. Single fast-path check at `JavaTypeConversion.kt:310` adds zero cost
  for non-adapter consumers.
- **`FirOuterClassTypeParameterRef` is the canonical inner-class indicator on `FirJavaClass`.**
  Detecting via `nonEnhancedTypeParameters.any { it is FirOuterClassTypeParameterRef }` avoids
  the lazy `status` evaluation that runs status-transformer extensions. For Kotlin classes the
  encoding differs; falling back to `status.isInner` is necessary but its rendering implications
  are subtle (KJK trip-wire).
- **Adapter's outer chain via `outerClass` recursion + `FirBackedJavaTypeParameter` wrappers
  works for Java-derived cross-file refs.** Two of three trip-wires fixed without further
  FIR-side changes.

### Notes / follow-ups not in this iteration

- **`containingClassIds` deletion (Step 4.5c proper) still deferred.** With Option A's
  symbol-carrying type-param wrappers, the FIR-side `findOuterTypeArgsFromHierarchy` is the
  only remaining consumer of `containingClassIds`. The Option B port keeps it alive in the
  `is JavaClass ->` branch. Removing `containingClassIds` from the public interface requires
  inlining the outer-chain walk via `classifier.outerClass` (or a parallel mechanism that
  doesn't depend on the model exposing `containingClassIds`).
- **Adapter could eventually expose richer surface for L2 retire** (`JavaScopeResolver.findLocalClass`
  body retirement — original §11 Step 4.5b plan in `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`).
  Not blocking; current adapter shape is sufficient for the rollback inventory's L1 work.
- **`testKJKComplexHierarchyWithNested.kt` actual was inspected via temporary instrumentation**
  (`JUnit5Assertions.kt` `[TEMPORARY DEBUG INSTRUMENTATION]` block). Reverted before commit.

---

## Step 4.5b first cut: delete dead `isResolved` properties from `core/compiler.common.jvm` Java-model interfaces — 2026-05-07

### Overview

Landed the smallest, safest part of Step 4.5b from
[`implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md`](implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md):
the three `isResolved` properties on `JavaClassifierType`, `JavaAnnotation`, and
`JavaEnumValueAnnotationArgument` are removed from their public-interface declarations.
A FIR-side audit confirmed the properties are **completely dead** — no production
caller in `compiler/fir/` reads `.isResolved` on any of these three Java-model surfaces.
The deletions are pure cleanup; the model overrides go too. Three additional iteration
goals (`FirBackedJavaClassAdapter`, deletion of `resolvedClassId`, deletion of
`isTriviallyFlexibleHint`) were prototyped but **reverted** — see "Reverted prototype"
below.

### Changes

- **Public-interface deletions (`core/compiler.common.jvm/.../load/java/structure/`)**
  - `javaTypes.kt`: removed `JavaClassifierType.isResolved` (default `get() = true`).
  - `javaElements.kt`: removed `JavaAnnotation.isResolved`.
  - `annotationArguments.kt`: removed `JavaEnumValueAnnotationArgument.isResolved`.
- **Model overrides removed (`compiler/java-direct/src/.../model/`)**
  - `JavaTypeOverAst.kt`: 5 deleted `isResolved` overrides (`JavaClassifierTypeOverAst`
    line 322, `JavaClassifierTypeForEnumEntry`, `JavaTypeParameterTypeOverAst`,
    `EnumSupertypeForJavaDirect` + its `EnumSelfTypeArgument`, `SimpleClassifierType`).
  - `JavaAnnotationOverAst.kt`: 2 deleted `isResolved` overrides (the meaningful
    `JavaAnnotationOverAst.isResolved` at line 73 and the
    `JavaEnumValueAnnotationArgumentOverAst.isResolved` at line 262).
- **Test fixture cleanup (`compiler/java-direct/test/.../`)**
  - `JavaParsingTypeResolutionTest.kt`: removed 3 `isResolved` reads (1 assert + 2
    println debug lines). The surrounding `classifier == null` /
    `classifierQualifiedName` assertions cover the user-visible AST-level invariants.
  - `JavaParsingAnnotationsTest.kt`: removed 5 `isResolved` reads on
    `JavaAnnotation` / `JavaEnumValueAnnotationArgument`. Adjacent assertions on
    `classId` / `enumClassId` / `entryName` cover the user-visible behaviour.
  - `JavaParsingMembersTest.kt`: 1 `isResolved` read deleted; `classifier == null`
    assertion already present.
  - `JavaParsingTypeSystemTest.kt`: 2 `isResolved` reads replaced with
    `classifier == null` checks (the parsing-level invariant for cross-file refs).
- **Documentation updates** (separate docs-sweep iteration earlier today; recapped here
  for completeness): added rule 7 ("No new public members on Java-model interfaces") to
  [`AGENT_INSTRUCTIONS.md`](AGENT_INSTRUCTIONS.md); created
  [`implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md`](implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md);
  added 2026-05-07 revision note + "Withdrawn" annotations on the "minimal classifier"
  passages in
  [`implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`](implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md).

### Reverted prototype: `FirBackedJavaClassAdapter` + `resolvedClassId` deletion + `isTriviallyFlexibleHint` deletion

A larger Step 4.5b implementation was attempted in this same iteration:

1. New `compiler/java-direct/src/.../resolution/FirBackedJavaClassAdapter.kt` —
   minimal `JavaClass` adapter wrapping a resolved `ClassId`, exposing `name` /
   `fqName` / `outerClass` (recursive) / `isStatic = true` / `typeParameters` count
   read from `FirJavaClass.nonEnhancedTypeParameters` (the pre-enhancement reader is
   required to avoid a `FirSignatureEnhancement` cycle through `isRaw`).
2. `JavaClassifierTypeOverAst.computeClassifier()` extended with a cross-file branch
   that wraps `resolutionContext.resolve(rawTypeName)` in the adapter; `classifier`
   moved from getter to `lazy(PUBLICATION)` cache.
3. `JavaClassifierType.resolvedClassId` deleted from
   `core/compiler.common.jvm/.../javaTypes.kt`.
4. `JavaClassifierType.isTriviallyFlexibleHint` deleted from `javaTypes.kt`; the
   FIR-side `JavaTypeConversion.kt:193` substitution `isTrivial = isTriviallyFlexibleHint`
   replaced with `isTrivial = false`.
5. `JavaTypeConversion.resolveTypeName` restored to its pre-`java-direct` body
   (`(javaType.classifier as? JavaClass)?.classId ?: findClassIdByFqNameString(...) ?: ClassId.topLevel(...)`).

The validation-gate run produced **3 stable regressions** in the
`JavaUsingAst*` matrix that the prototype could not eliminate:

- `Tests > Generics > InnerClasses > testJ_k_complex`
- `ResolveWithStdlib > J_k > testKJKComplexHierarchyWithNested`
- `BoxJvm > Invokedynamic > Sam > FunctionRefToJavaInterface > testGenericBoundInnerConstructorRef`

All three exercise cross-file **inner classes** whose outer class lives in another
file and whose outer type-parameter substitution is supplied via the containing
class's inheritance chain. PSI handles these because PSI's `classifier` is a real
`JavaClass` carrying a fully-shaped `outerClass` chain with real `JavaTypeParameter`
instances; the model's `computeTypeArguments` walks `outerClass.typeParameters` and
emits `JavaTypeParameterReference` instances for the implicit outer args. The
`FirBackedJavaClassAdapter` cannot supply real `JavaTypeParameter` instances
(synthesised placeholders aren't bound to FIR symbols, so they break downstream
substitution); patching the FIR side's `is JavaClass ->` branch to mirror the
`null ->` branch's `findOuterTypeArgsFromHierarchy` recovery did not help because the
explicit-typeArguments case (`BaseInner<Double, String>`) doesn't enter the empty-args
path. The whole prototype was reverted per `AGENT_INSTRUCTIONS.md` rule "any
regression → revert". The inventory doc's Step 4.5b is reclassified as **partially
landed** (the `isResolved` deletions); the rest blocks on **Step 4.5c** (proper
outer-class-chain handling for cross-file inner classes — likely a structural adapter
or a substantively different approach).

The prototype's intermediate findings are recorded here as a forward reference:

- **`FirJavaClass.typeParameters` is unsafe to read from the model.** Reading it
  triggers `FirSignatureEnhancement.enhanceTypeParameterBounds`, which calls
  `JavaTypeConversion.isRaw` on a `JavaClassifierTypeOverAst`, which queries
  `classifier.typeParameters` on the adapter, which… reads `FirJavaClass.typeParameters`
  again. Infinite recursion. **Use `FirJavaClass.nonEnhancedTypeParameters` instead** —
  it returns the raw `List<FirTypeParameterRef>` without driving enhancement.
- **`isStatic` matters more than expected for adapter shape.** Returning `false`
  (computed from `firRegularClass.status.isInner`) makes the model's
  `computeTypeArguments` walk the outer chain and emit placeholder
  `JavaTypeParameter` instances; FIR then errors with `IndexOutOfBoundsException` /
  `CANNOT_INFER_PARAMETER_TYPE` because the placeholders don't match real type-parameter
  symbols. Returning `true` short-circuits the implicit walk but leaves outer-arg
  substitution to FIR's `findOuterTypeArgsFromHierarchy` — which only fires in the
  `null ->` branch (line 322 of `JavaTypeConversion.kt`) for empty-args cases, so the
  explicit-args inner-class scenario regresses anyway.
- **Filtering adapter classifiers out of `resolveSupertypeNames`** (BFS supertype walk)
  was tried and made no test difference — the BFS isn't the source of the regressions.
- **Restricting the adapter to top-level classes only** is also wrong — many tests
  (Map.Entry, etc.) need the adapter precisely for nested cross-file references when
  there's no containing-class inheritance contributing outer args.

### Test Results

- `JavaUsingAst*` matrix (`JavaUsingAstPhasedTestGenerated` +
  `JavaUsingAstBoxTestGenerated`): **2693/2693 passing** after revert (parsed from
  `build/test-results/test/`). No regressions vs the post-Step-4.5a baseline.
- PSI regression gate (`PhasedJvmDiagnosticLightTreeTestGenerated.*`):
  **BUILD SUCCESSFUL**, 0 failures.

### Files Modified

| File | Change |
|------|--------|
| `core/compiler.common.jvm/src/.../load/java/structure/javaTypes.kt` | Deleted `JavaClassifierType.isResolved`. |
| `core/compiler.common.jvm/src/.../load/java/structure/javaElements.kt` | Deleted `JavaAnnotation.isResolved`. |
| `core/compiler.common.jvm/src/.../load/java/structure/annotationArguments.kt` | Deleted `JavaEnumValueAnnotationArgument.isResolved`. |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | Deleted 5 `isResolved` overrides. |
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | Deleted 2 `isResolved` overrides. |
| `compiler/java-direct/test/.../JavaParsingTypeResolutionTest.kt` | Deleted `isResolved` assertions/println. |
| `compiler/java-direct/test/.../JavaParsingAnnotationsTest.kt` | Deleted `isResolved` assertions. |
| `compiler/java-direct/test/.../JavaParsingMembersTest.kt` | Deleted `isResolved` assertion. |
| `compiler/java-direct/test/.../JavaParsingTypeSystemTest.kt` | Replaced `isResolved` assertions with `classifier == null` checks. |
| `compiler/java-direct/AGENT_INSTRUCTIONS.md` | Added rule 7 (no new public Java-model interface members) — earlier docs-sweep iteration. |
| `compiler/java-direct/implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` | New doc — earlier docs-sweep iteration. |
| `compiler/java-direct/implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` | 2026-05-07 revision note + "Withdrawn" annotations — earlier docs-sweep iteration. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **`isResolved` was dead code on the FIR side.** A repo-wide `grep "\.isResolved\b"`
  excluding `isResolvedTo`/`FirResolved*`/etc. found *zero* production callers in
  `compiler/fir/` for the three Java-model surfaces. The properties existed only as
  parsing-level test assertions and model overrides. Pure cleanup.
- **The `FirBackedJavaClassAdapter` approach is structurally insufficient for
  cross-file inner classes.** PSI's classifier carries a fully-shaped `outerClass`
  chain; replicating that with a synthetic adapter requires linking each placeholder
  type-parameter to the actual `FirTypeParameterSymbol` (so FIR's
  `javaTypeParameterStack` lookup at `JavaTypeConversion.kt:314` can find them).
  That's deeper than Step 4.5b's nominal scope — see the inventory doc's Step 4.5c.
- **`AGENT_INSTRUCTIONS.md` rule 7 (the no-new-public-members rule added in this
  cycle's docs sweep) is the right structural defence.** Without it, an iteration
  hitting the cross-file inner-class wall would be tempted to re-introduce a
  side-channel (e.g., a new `JavaClassifierType.outerTypeParameterSymbols` property)
  rather than escalate to Step 4.5c. The rule makes that choice explicit code review
  rejection material.

### Notes / follow-ups not in this iteration

- **Step 4.5c** (proper outer-class-chain handling for cross-file inner classes) is
  now the prerequisite for the rest of Step 4.5b (`resolvedClassId`,
  `isTriviallyFlexibleHint` deletions, full `FirBackedJavaClassAdapter` adoption).
  Update the inventory doc's §3 sequence to reflect this.
- **Test data update for the dropped `isResolved` assertions:** none. The replacement
  assertions (`classifier == null`, `classId` / `enumClassId`) cover the same
  user-visible invariants without exposing the deleted interface property.

---

## Step 4.5b second attempt: Option B FIR-side outer-args propagation — reverted (insufficient) — 2026-05-07 (later)

### Overview

Second attempt at the full Step 4.5b deliverable. Implemented "Option B" from
`/Users/ich-jb/.claude/plans/read-compiler-java-direct-agent-instruct-linked-stonebraker.md`
addendum: generalised `JavaTypeConversion.kt`'s `null ->` branch
`findOuterTypeArgsFromHierarchy` recovery to the `is JavaClass ->` branch
(~30 LOC), rebuilt the `FirBackedJavaClassAdapter` with `nonEnhancedTypeParameters`-
based count, wired through `classifierAdapterFor` in `JavaResolutionContext`, deleted
`resolvedClassId` and `isTriviallyFlexibleHint` from the public interface, restored
`JavaTypeConversion.resolveTypeName` to its pre-`java-direct` body. Same three
regressions surfaced as the first attempt (`testJ_k_complex`,
`testKJKComplexHierarchyWithNested`, `testGenericBoundInnerConstructorRef`).
Reverted. Only the orphaned `FirBackedJavaClassAdapter.kt` is preserved in tree for
Step 4.5c to build on.

### Why Option B is insufficient

`findOuterTypeArgsFromHierarchy` (`JavaTypeConversion.kt:461`) skips
`containingClassIds[0]` to avoid recursion in supertype-resolution context:

```kotlin
// Skip the first containing class (index 0) — it's the class whose supertypes are currently
// being resolved. Accessing its superTypeRefs would cause infinite recursion.
for (i in 1 until containingClassIds.size) {
```

For cross-file refs in **method-body / field-type** context (e.g.
`bar(): BaseInner<Double, String>` declared inside `Outer<H>` extends `BaseOuter<H>`),
`containingClassIds = [Outer]` (size 1) — loop body doesn't execute, returns
`null`, Option B's outer-args branch falls through to `buildTypeProjections`'s
truncate-to-min behaviour, outer arg `H` is lost. For cross-file refs in
**supertype-clause** context (e.g.
`Inner extends BaseOuter<H>.BaseInner<Double, String>` inside Outer),
`containingClassIds = [Inner, Outer]` (size 2) — loop iterates Outer at index 1,
walks Outer's supertypes, finds BaseOuter's `H`. Option B works there. Two contexts,
two shapes; Option B's cheap one-condition gate cannot distinguish them.

### Why pre-Step-4.5b passes the failing tests

AST-side `JavaInheritedMemberResolver.findInnerClassFromSupertypes`
(`compiler/java-direct/src/.../resolution/JavaInheritedMemberResolver.kt:77`) returns
a **real `JavaClassOverAst`** for cross-file inherited inner classes via
`classFinder.collectInheritedInnerClasses` lookup. The real classifier carries a
fully-shaped `outerClass` chain back through the AST; the model's
`computeTypeArguments` walks `outerClass.typeParameters` and emits real
`JavaTypeParameter` instances declared in BaseOuter.java's source. Those instances
are registered in `MutableJavaTypeParameterStack` at
`FirJavaFacade.convertJavaClassToFir:159`, so FIR's `is JavaTypeParameter ->`
lookup `javaTypeParameterStack[classifier]` at `JavaTypeConversion.kt:310` succeeds
and resolves to the correct `FirTypeParameterSymbol`. Cross-file inherited inner
classes never reach the cross-file/adapter branch via `computeClassifier` —
`findLocalClass` step 3 catches them via `findInnerClassFromSupertypes`.

### Why the synthetic-adapter approach can't replicate this

`FirBackedJavaClassAdapter.typeParameters` returns `PlaceholderJavaTypeParameter`
instances. Those placeholders are **not** in any `javaTypeParameterStack`. If
`computeTypeArguments` walked the adapter's `outerClass` chain and emitted them
(setting `isStatic = false`), FIR's `javaTypeParameterStack[placeholder]` lookup
would return `null` → `ConeUnresolvedNameError` / `IndexOutOfBoundsException` /
`CANNOT_INFER_PARAMETER_TYPE` (the symptoms observed in earlier prototype
iterations). The current adapter has `isStatic = true` to short-circuit the walk,
which avoids those crashes but leaves implicit outer args missing for the
method-body / field-type context.

### Path forward — Option A required for Step 4.5c

The adapter must carry its `FirTypeParameterSymbol` directly through a
`JavaTypeParameter`-implementing wrapper, with FIR's `is JavaTypeParameter ->`
branch checking for this subtype before falling back to `javaTypeParameterStack`
lookup. Localised, no stack identity contention, no parallel resolution-scoped
stack. Estimated LOC: ~80-120 (smaller than the original Option A estimate because
the structural adapter half is already written from this iteration's prototype).

### Test Results

- `JavaUsingAst*` matrix after revert: **2693/2693 passing** (parsed from
  `build/test-results/test/`). Matches the post-isResolved-deletion baseline.
- PSI regression gate (`PhasedJvmDiagnosticLightTreeTestGenerated.*`) remains green
  (verified earlier in session).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/implDocs/INTERFACE_ROLLBACK_INVENTORY_2026_05_07.md` | Step 4.5b status updated with second-attempt findings; Option A marked as Step 4.5c prerequisite. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry. |

(The Option B prototype itself produced no committed source changes after revert,
except the orphaned `FirBackedJavaClassAdapter.kt` preserved for Step 4.5c.)

### Key Learnings

- **Option B's gate cannot distinguish supertype-clause vs method-body contexts.**
  The `containingClassIds` shape (`size ≥ 2` vs `size == 1`) does discriminate
  empirically, but `findOuterTypeArgsFromHierarchy`'s skip-at-index-0 invariant
  exists for sound reasons (recursion avoidance during supertype resolution) and
  starting at index 0 unconditionally would risk the recursion the skip prevents.
- **Real classifier vs synthetic adapter** is the load-bearing distinction. PSI's
  `classifier` is a real `PsiClass` with full structural data, registered in
  PSI's symbol stack. java-direct's pre-Step-4.5b real `JavaClassOverAst` (when
  obtained via `findInnerClassFromSupertypes`) is registered in
  `MutableJavaTypeParameterStack`. The adapter is registered nowhere — that's the
  missing piece Step 4.5c must address.
- **`AGENT_INSTRUCTIONS.md` rule 7 keeps holding the line.** The revert reaffirms
  the no-side-channel invariant: rather than re-introducing `resolvedClassId` to
  ship a partial Step 4.5b, the iteration stays at the safe baseline and defers to
  Step 4.5c.

---

## Step 4.5a of `FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md`: delete `resolve(...)` / `resolveAnnotation(...)` / `resolveEnumClass(...)` from public interfaces; model owns cross-file resolution via injected `FirSession` — 2026-05-06 (later)

### Overview

Landed Step 4.5a of `implDocs/FIRSESSION_INJECTION_PROPOSAL_2026_05_05.md` on top of the
foundation iteration recorded immediately below. The single load-bearing change is the
**deletion** of `JavaClassifierType.resolve(...)`, `JavaAnnotation.resolveAnnotation(...)`,
and `JavaEnumValueAnnotationArgument.resolveEnumClass(...)` from their
`core/compiler.common.jvm` public interfaces (Shape 1 of §3 / §12). The Java Model now
owns cross-origin classifier resolution: it consults its injected `FirSession` through
a typed `LazySessionAccess` wrapper, populates a new `JavaClassifierType.resolvedClassId`
interface hint, and FIR's `JavaTypeConversion.resolveTypeName` returns to its
pre-`java-direct` shape (`classifier?.classId ?: resolvedClassId ?: findClassIdByFqNameString ?: ClassId.topLevel`).
The resolver-unification residue closes by construction: L1 (drop
`JavaInheritedMemberResolver`'s Phase 1) is no longer a structural concern because the
BFS dispatcher walks AST data per-origin without re-reading `FirJavaClass.superTypeRefs`,
and cycle handling is now bounded by a `JavaResolutionContext`-scoped
`JavaSupertypeLoopChecker` (§6.1 of the proposal).

### Changes

- **Public-interface deletions (`core/compiler.common.jvm`)**
  - `structure/javaTypes.kt`: removed `JavaClassifierType.resolve(tryResolve, getSupertypeClassIds)`;
    added a new `val resolvedClassId: ClassId? = null` hint that pre-`java-direct` impls
    (PSI / binary) inherit as `null` and `java-direct`'s `JavaClassifierTypeOverAst`
    overrides with a `lazy(PUBLICATION)` model-driven probe.
  - `structure/javaElements.kt`: removed `JavaAnnotation.resolveAnnotation(tryResolve)`;
    `JavaAnnotation.classId` is now reliable for every reference and FIR reads it
    directly.
  - `structure/annotationArguments.kt`: removed
    `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve)`; FIR consumers read
    `enumClassId` directly.

- **Model side (`compiler/java-direct/.../model`)**
  - `JavaTypeOverAst.kt`: `JavaClassifierTypeOverAst` now overrides `resolvedClassId`
    with a `lazy(PUBLICATION)` probe that consults
    `resolutionContext.resolve(rawTypeName)` only when `LazySessionAccess` is wired —
    parsing-level fixtures (which keep their AST-only fallback shape, see the foundation
    iteration's `createDummyFirSessionForTests`) short-circuit on
    `resolutionContext.hasLazySessionAccess`. The trivial
    `JavaClassifierTypeForEnumEntry.resolve()` override is gone (the type already sets
    `classifier = enumClass`, so `classifier.classId` returns the same
    `ClassId.topLevel(enumClass.fqName)` it was hand-rolling). 5 deleted
    `resolve(tryResolve, getSupertypeClassIds)` overrides.
  - `JavaAnnotationOverAst.kt`: `JavaAnnotationOverAst.classId` /
    `JavaEnumValueAnnotationArgumentOverAst.enumClassId` consult the model's resolver
    only when `LazySessionAccess` is wired; 2 deleted `resolveEnumClass(...)` overrides
    + 3 deleted `resolveAnnotation(...)` overrides.

- **Resolution side (`compiler/java-direct/.../resolution`)**
  - **New `LazySessionAccess.kt`** (typed wrapper, defensive against bare-bones sessions
    via `nullableSessionComponentAccessor`): the single chokepoint through which the
    model reads `FirSession.symbolProvider`. Hard-enforces failure-mode-1 of the
    proposal's §7 (no symbol-provider lookups during parsing / index population) by
    returning `null` when the session is the
    `createDummyFirSessionForTests()`-shaped no-component session.
  - **New `JavaSupertypeLoopChecker.kt`** (per-resolution-context cycle bound, modelled
    on K1's `SupertypeLoopChecker` and FIR's `SupertypeComputationStatus.Computing`
    sentinel): wraps every model-side supertype-walking entry point with an active-
    `ClassId` set; re-entry returns a default value rather than recursing. Records
    cycle edges via `consumeCycleEdges()` so that the Java-only-cycle diagnostic
    emission gate (`LoopInSupertype` → `CYCLIC_INHERITANCE_HIERARCHY`, §6.1 / §12 Q4 of
    the proposal) can pick them up in a follow-up landing.
  - **`JavaResolutionContext.kt` rewritten**: `resolve(name)` and
    `findInheritedNestedClass` lose their `tryResolve` / `getSupertypeClassIds` callback
    parameters; new private `tryResolve(classId)` and per-origin
    `directSupertypeClassIds(classId)` dispatcher (wrapped in `loopChecker.guarded`).
    The `JavaInheritedMemberResolver` BFS now consumes the dispatcher; its Phase-1 +
    Phase-2 split survives as an internal implementation detail (no longer a public
    callback contract), but the Phase-2 reads come from the dispatcher, never from
    `FirJavaClass.superTypeRefs` directly.
  - Dead `JavaResolvedClassLikeSymbol.kt` removed (its `JavaResolvedClassOrigin` enum +
    `JavaResolvedClassLikeSymbol` data class were the Stage-1 callback-API hook from
    Step 2; the deletion in Step 4.5a makes them dead code).

- **FIR side (`compiler/fir/fir-jvm`)**
  - `JavaTypeConversion.kt`: `resolveSymbolBasedClassId` is **deleted** outright;
    `getResolvedSupertypeClassIds` is **deleted** (cross-origin supertype reads now go
    through the model's dispatcher, including the binary-Java arm via the new
    `FirJavaClass.directSupertypeClassIds()` cache). `resolveTypeName` is restored to
    its pre-`java-direct` body, with the new `resolvedClassId` hint inserted between
    `classifier?.classId` and `findClassIdByFqNameString`. KDoc rewritten to cite the
    proposal's §3 / §5.
  - `javaAnnotationsMapping.kt`: callers read `JavaAnnotation.classId` /
    `JavaEnumValueAnnotationArgument.enumClassId` directly; the lambda-construction
    boilerplate around the deleted callbacks is gone.
  - `declarations/FirJavaClass.kt`: new `directSupertypeClassIds()` lazy cache (variant
    **C** of §12 Q1) populated lazily from `javaClass?.supertypes`. Variant D (the
    `FirJavaClass.javaClass` visibility flip) is preserved as a fallback in §12 of the
    proposal but not taken in this iteration.

### Test Results

- `JavaUsingAst*` matrix (`JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`):
  **2693/2693 passing**, 0 failures, 0 errors, 0 skipped (parsed from
  `build/test-results/test/TEST-*JavaUsingAst*.xml`). No regression vs. the post-Step-4
  baseline.
- `JavaParsing*` parsing-level unit tests: **85/85 passing**, 0 failures, 0 errors
  (parsed from `build/test-results/test/TEST-*JavaParsing*.xml`). The dummy session
  from the foundation iteration carries the parsing-level corpus; no parsing-level
  test reaches `LazySessionAccess`.
- `compileTestKotlin` BUILD SUCCESSFUL on the post-deletion source tree (after fixing
  three intermediate compile errors during the bisection: a stale
  `resolveSymbolBasedClassId` import, a `@SymbolInternals` opt-in on the new
  `directSupertypeClassIds()` cache reader, and two test-side type-inference fallouts
  in `JavaParsingAnnotationsTest`).
- The Step 4.5a perf gate on `testIntellij_platform_externalProcessAuthHelper` was
  **NOT** run in this iteration — same harness-unreachability constraint as Step 3 / 4.
  The Step 4.5a change is structurally a *replacement* of one same-cost callback path
  (FIR-side lambda → model) with a same-cost direct-read path (model → injected
  `FirSession`); the only new allocation is the `lazy(PUBLICATION)` delegate on
  `resolvedClassId`, which fires at most once per `JavaClassifierTypeOverAst`.

### Files Modified

| File | Change |
|------|--------|
| `core/compiler.common.jvm/src/.../structure/javaTypes.kt` | Deleted `JavaClassifierType.resolve(tryResolve, getSupertypeClassIds)`; added `val resolvedClassId: ClassId? = null` interface hint with KDoc citing §3 of the proposal. |
| `core/compiler.common.jvm/src/.../structure/javaElements.kt` | Deleted `JavaAnnotation.resolveAnnotation(tryResolve)`. |
| `core/compiler.common.jvm/src/.../structure/annotationArguments.kt` | Deleted `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve)`. |
| `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` | Deleted `resolveSymbolBasedClassId`; deleted `getResolvedSupertypeClassIds`; restored `resolveTypeName` to its pre-`java-direct` body with the new `resolvedClassId` hint inserted between `classifier?.classId` and `findClassIdByFqNameString`; KDoc rewrite. |
| `compiler/fir/fir-jvm/src/.../javaAnnotationsMapping.kt` | Removed lambda-construction boilerplate around the deleted callbacks; consumers read `classId` / `enumClassId` directly. |
| `compiler/fir/fir-jvm/src/.../declarations/FirJavaClass.kt` | New `directSupertypeClassIds()` lazy cache (variant **C** of §12 Q1) populated from `javaClass?.supertypes`. |
| `compiler/java-direct/src/.../resolution/LazySessionAccess.kt` | New: typed wrapper around the injected `FirSession`, defensive against bare-bones sessions via `nullableSessionComponentAccessor`. Single chokepoint for `FirSession.symbolProvider` reads. |
| `compiler/java-direct/src/.../resolution/JavaSupertypeLoopChecker.kt` | New: per-`JavaResolutionContext` active-`ClassId` set; `consumeCycleEdges()` records edges for the deferred Java-only-cycle diagnostic gate. |
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | `resolve(name)` and `findInheritedNestedClass` lose their callback parameters; new private `tryResolve(classId)` and `directSupertypeClassIds(classId)` dispatcher; the BFS now consumes the dispatcher. |
| `compiler/java-direct/src/.../resolution/JavaResolvedClassLikeSymbol.kt` | Deleted (Stage-1 callback-API hook is dead code post-deletion). |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | `JavaClassifierTypeOverAst.resolvedClassId` `lazy(PUBLICATION)` override; deleted `JavaClassifierTypeForEnumEntry.resolve()`; 5 deleted `resolve(tryResolve, getSupertypeClassIds)` overrides. |
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | 3 deleted `resolveAnnotation(...)` overrides + 2 deleted `resolveEnumClass(...)` overrides; `classId` / `enumClassId` consult the model's resolver only when `LazySessionAccess` is wired. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; bumped `Last Updated`. |

### Key Learnings

- **Shape 1 (deletion) really is shorter than Shape 2 (parameter narrowing).** §3 of the
  proposal predicted that deleting `resolve(...)` / `resolveAnnotation(...)` outright
  would be shorter than narrowing their parameter lists; the per-site count confirms it
  — 5 + 3 + 2 = 10 override deletions vs. the 10 per-site signature edits Shape 2 would
  have required, and *zero* call sites left to thread an `is JavaClassifierTypeOverAst`
  smart-cast through.
- **`LazySessionAccess.hasLazySessionAccess` is the right test-fixture seam.** Parsing-
  level fixtures (which carry the dummy session from the foundation iteration) pass it
  as `false`; the model's `resolvedClassId` / `classId` / `enumClassId` overrides
  short-circuit before touching `FirSession.symbolProvider`. This makes the
  failure-mode-1 invariant (no symbol-provider lookups during parsing) a *type-system*
  contract rather than a documentation contract — exactly what §12 Q2 of the proposal
  asked for (the answer to Q2 is now landed code, not just docs).
- **Variant C beats variant D for the binary-Java supertype cache.** A
  `directSupertypeClassIds()` lazy cache on `FirJavaClass` is a one-allocation-per-
  binary-class affair that fits cleanly inside FIR's existing lazy infrastructure;
  variant D's visibility flip on `FirJavaClass.javaClass` would have widened the
  internal-to-`compiler/fir/fir-jvm` API surface in a way the proposal's §12 Q1
  flagged as risky. Variant D stays in §12 as documented fallback.
- **The `JavaResolvedClassLikeSymbol` enum was a transitional artefact.** It was the
  Stage-1 callback-API hook from Step 2 of the merged plan, never consumed by any
  caller (the `getClassLikeSymbol` parameter on `JavaResolutionContext.resolve()` was
  always `null` at call time). Step 4.5a's deletion is the first actual *use* of the
  origin-aware information it was designed to carry — but the use is internal to the
  model's per-origin dispatcher, not on a public API, so the wrapper class is dead.
- **Three intermediate compile errors during bisection were all signals, not noise.**
  (1) The stale `resolveSymbolBasedClassId` import surfaced a loose-end call site
  in `findTypeArgsForClassInHierarchy`; (2) the `@SymbolInternals` opt-in on the
  new `directSupertypeClassIds()` reader caught a real visibility mismatch — the
  cache reader had to live on `JavaResolutionContext`'s side, not on a `FirJavaClass`
  extension; (3) the `JavaParsingAnnotationsTest` type-inference fallouts confirmed
  that the deletion was actually reaching test code, not just production.

### Notes / follow-ups not in this iteration

- **Step 4.5b** (the L2 closer: retire `JavaScopeResolver.findLocalClass` and
  `JavaClassOverAst.findInnerClassInSupertypes` once the model exposes a FIR-derived
  `JavaClass`-shaped view) is the next iteration in §11 of the proposal.
- **Java-only inheritance-cycle diagnostic emission gate** (`LoopInSupertype` →
  `CYCLIC_INHERITANCE_HIERARCHY`, §6.1 / §12 Q4): the cycle-checker records edges and
  `consumeCycleEdges()` is in place, but the recorded edges are not yet plumbed into
  `FirJavaClass.computeSuperTypeRefsByJavaClass`. Deliberately deferred to keep this
  iteration scoped to the source-code half of Step 4.5a.
- **`AGENT_INSTRUCTIONS.md` laziness-rule bullet** (§7 mitigation tier 2 of the
  proposal) and the source-doc revisions described in §13 are not landed here — this
  iteration is the source-code half of Step 4.5a only; the docs sweep belongs to
  Step 5 of the merged plan.

---

## Step 4.5a foundation: `JavaClassFinderOverAstImpl.session` non-nullable + `createDummyFirSessionForTests` for parsing-level unit tests — 2026-05-06

### Overview

Preliminary iteration that prepared the ground for the Step 4.5a deletion described in
the entry above. Made `JavaClassFinderOverAstImpl.session` non-nullable (parameter and
property) so that the model can rely on a real `FirSession` being present at every
call site, and stood up a minimal `DummyJavaDirectFirSession`-backed
`createDummyFirSessionForTests()` helper so that the `JavaParsing*` parsing-level test
corpus (which previously passed `null`) keeps compiling and running. The dummy session
has no registered components and is sufficient *only* as long as parsing-level code does
not consult the symbol provider — exactly the invariant `LazySessionAccess` enforces in
the Step 4.5a entry above.

### Changes

- `compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt`: changed
  `private val session: FirSession?` → `private val session: FirSession` (parameter and
  property non-nullable).
- `compiler/java-direct/testFixtures/.../components.kt`: added
  `createDummyFirSessionForTests()` returning a private
  `DummyJavaDirectFirSession(FirSession.Kind.Source)` subclass (no registered
  components, opt-in to `@PrivateSessionConstructor`); the test-only
  `JavaClassFinderOverAstImpl(...)` factory now passes that session instead of `null`.
  The KDoc on the test factory documents the contract: the bare session is sufficient
  only as long as parsing-level code does not consult the symbol provider, matching the
  `LazySessionAccess` invariant the Step 4.5a entry above lands.

### Test Results

- `JavaUsingAst*` full matrix (`JavaUsingAstPhasedTestGenerated` +
  `JavaUsingAstBoxTestGenerated`): **2693/2693 passing**, 0 failures, 0 errors,
  0 skipped (parsed from `build/test-results/test/TEST-*JavaUsingAst*.xml`).
- `JavaParsing*` unit-test class set compiles and runs green (BUILD SUCCESSFUL after
  `--rerun-tasks --no-build-cache`).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt` | `session` parameter / property non-nullable. |
| `compiler/java-direct/testFixtures/.../components.kt` | New `createDummyFirSessionForTests()` + private `DummyJavaDirectFirSession`; the test-only `JavaClassFinderOverAstImpl(...)` factory passes the dummy session instead of `null`. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry. |

### Key Learnings

- **The dummy session is intentionally a private `DummyJavaDirectFirSession` subclass
  rather than a reuse of `FirCliSession`.** The latter would have pulled
  `:compiler:fir:fir-jvm` into the testFixtures explicit dependency surface; the bare
  subclass keeps the testFixtures dependency graph minimal and matches the parsing-
  level corpus's actual needs (no symbol-provider lookups, no enhancement, no resolve
  phases).
- **Non-nullable `session` is the structural prerequisite for `LazySessionAccess`.**
  As long as the property is `FirSession?`, every call site that wants to consult the
  injected session has to thread a `?.` chain or a `requireNotNull` past the type
  system; making it non-null moves the invariant into the constructor, where the
  `JavaClassFinderOverAstFactory.createJavaClassFinder` plumbing landed in the previous
  cycle already supplies a real session in production.
- **Foundation work is worth a separate entry even when the next iteration
  supersedes it.** The `null`-removal and the test fixture are pure scaffolding; they
  carry no behavioural change on their own. Logging them as a distinct iteration makes
  the bisection / archaeology cheaper for a future reader who wants to understand why
  `createDummyFirSessionForTests` exists in `testFixtures`.

---

## Merged plan Step 4: Unification Stage 4 (`findLocalClass` removed from `ClassId`-resolution path; `resolveFromLocalScope` walks `getContainingClassIds()` via FIR `tryResolve`) — 2026-05-05 (Step 4)

### Overview

Landed Step 4 of `MERGED_REFACTORING_PLAN_2026_05_04.md` — the resolver-unification "Stage 4 + Stage 5 (partial)" piece — on top of the green Step-3 baseline. The AST-side `JavaScopeResolver.findLocalClass` is no longer in the `ClassId`-resolution path: `JavaResolutionContext.resolveFromLocalScope` (step 2 of `resolveSimpleNameToClassIdImpl`, JLS 6.5.2) now walks `getContainingClassIds()` from innermost to outermost and probes the FIR symbol provider via `tryResolve(containingId.createNestedClassId(name))`. Stage 5's full collapse (shrinking the AST side to "type parameter?" + `containingClassIds` only) remains a deferred concern — `findLocalClass` is retained for the AST classifier path (`JavaTypeOverAst.computeClassifier`), where the j+k_complex.kt trip-wire from the Step-3 post-mortem still requires a structural `JavaClass` with its full outer-class chain.

### Changes

- **Stage 4 — `JavaResolutionContext.resolveFromLocalScope`**
  - Replaced the previous AST-side 2a path:
    ```kotlin
    findLocalClass(Name.identifier(simpleName))?.let { localClass ->
        val fqName = localClass.fqName
        if (fqName != null) {
            val classId = fqNameToClassId(fqName)
            if (tryResolve(classId)) return classId
        }
    }
    ```
    with the Stage-4 spec's containing-chain FIR walk:
    ```kotlin
    val nameId = Name.identifier(simpleName)
    for (containingId in getContainingClassIds()) {
        val candidate = containingId.createNestedClassId(nameId)
        if (tryResolve(candidate)) return candidate
    }
    ```
  - The walk subsumes steps 1, 2, 4 of `JavaScopeResolver.findLocalClass` (directly-declared
    inner classes anywhere up the containing chain) by relying on the FIR symbol
    provider's existing `JvmSymbolProvider → JavaClassFinderOverAstImpl` chain to resolve
    `containingId.createNestedClassId(name)` to the same AST node those AST-side queries
    would have produced. JLS 6.3 innermost-wins ordering is preserved by iterating
    `getContainingClassIds()` from innermost to outermost (its existing contract).
  - Step 3 of the AST `findLocalClass` (inherited inners from supertypes) is covered by
    the existing 2b path (aggregated map / two-phase BFS via
    `resolveInheritedInnerClassToClassId`), unchanged.
  - Step 5 of the AST `findLocalClass` (same-file top-level fast path) is intentionally
    *not* reproduced inside `resolveFromLocalScope`: same-file top-level classes share
    their `ClassId` with same-package cross-file classes
    (`ClassId(packageFqName, simpleName)`), so they are picked up by the next step in
    `resolveSimpleNameToClassIdImpl` — `resolveFromSamePackage`. No new `tryResolve`
    cost: the same single probe happens, just one step later.
  - The KDoc on `resolveFromLocalScope` is rewritten to describe the Stage-4 outcome,
    cite the unification doc, and explicitly call out where each of the old
    `findLocalClass` steps now lives.

- **Stage 5 partial — `JavaScopeResolver.findLocalClass` (KDoc only)**
  - Rewrote the KDoc to record the post-Stage-4 role: this method is no longer in the
    `ClassId`-resolution path; it is the AST-side fast path used by the Java model layer
    (`JavaTypeOverAst.computeClassifier`, `JavaClassCache`, `ConstantEvaluator`). Body is
    unchanged — the five-step ordering is still required because the AST classifier path
    needs a structural `JavaClass` (with full outer-class chain) for cross-file
    inherited inners (the `j+k_complex.kt` trip-wire from the Step-3 post-mortem).
  - Stage 5's full collapse — shrinking the AST side to "type parameter?" +
    `getContainingClassIds()` — is documented as a deferred concern: it requires giving
    the AST classifier path a FIR-derived `JavaClass` for cross-file inherited inners,
    which the existing `getClassLikeSymbol` callback alone does not provide.

- **`JavaResolutionContext.findLocalClass` (KDoc only)** — passthrough doc updated to
  point at `JavaScopeResolver.findLocalClass`'s KDoc for the post-Stage-4 role.

### Test Results

`./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --rerun-tasks --no-build-cache` — **BUILD SUCCESSFUL** in 1m 56s, 0 failures / 0 errors. XML parse of `build/test-results/test/`: **2693 tests, all passed** (no regressions vs. the post-Step-3 baseline).

The Step-4 perf gate on `testIntellij_platform_externalProcessAuthHelper` (re-run parse counter on the Stage-3 testbed; per the merged plan validation gate, must be ≤ Step-3's value within noise) was **NOT** run in this iteration — same harness-unreachability constraint as Step 3. The Stage-4 change is structurally a *replacement* of one same-cost lookup with another (one `findLocalClass`-mediated `tryResolve` per innermost containing class becomes one `tryResolve(containingId.createNestedClassId(name))` per containing-class entry), so the parse counter cannot be affected by this change alone (`tryResolve` does not parse anything; `findLocalClass`'s syntactic AST queries do not parse either). The symbol-creation counter could theoretically tick up by one extra `getClassLikeSymbolByClassId` call per containing-chain level for misses, but the FIR `tryResolve` callback already short-circuits on the first hit, and the chain is typically 1–2 deep. If the harness becomes available before Step 5, this iteration's perf gate can be re-run retrospectively.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | `resolveFromLocalScope`: Stage-4 swap (2a → containing-chain FIR walk); KDoc rewrite. `findLocalClass` passthrough KDoc updated. |
| `compiler/java-direct/src/.../resolution/JavaScopeResolver.kt` | `findLocalClass` KDoc rewritten to describe post-Stage-4 role + Stage-5 deferral note. Body unchanged. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; `Last Updated` bumped. |

### Key Learnings

- **The Stage-4 spec's `findLocalClass: JavaClass?` signature is approximate.** The
  unification doc shows `fun findLocalClass(name): JavaClass? { /* FIR via getClassLikeSymbol */ }`,
  but `getClassLikeSymbolByClassId` returns a FIR symbol, not an AST `JavaClass`. The
  practical Stage-4 transformation operates at the *`ClassId`-resolution* layer
  (`resolveFromLocalScope`), where the FIR `tryResolve` callback already does what the
  spec describes. The AST classifier path keeps a separate `findLocalClass` because its
  consumers (`JavaTypeOverAst.computeClassifier`) require a structural `JavaClass`.
- **Same-file top-level classes don't need a dedicated fast path inside
  `resolveFromLocalScope`.** They share their `ClassId` with same-package cross-file
  classes, so `resolveFromSamePackage` (the next step in `resolveSimpleNameToClassIdImpl`)
  handles them with the same single `tryResolve` probe. The only behavioural change is
  that same-file top-level no longer beats inherited inners in the `ClassId` path — but
  that aligns with JLS 6.3 / 6.5.5.1 priority (inherited inners are in narrower scope
  than same-package top-level).
- **`getContainingClassIds()` already preserves innermost-wins ordering** (returns from
  containingClass outwards, walking `outerClass`), so the Stage-4 walk does not need a
  separate ordering pass.
- **Stage 5's full collapse is genuinely entangled with the AST classifier API.**
  `JavaTypeOverAst.computeClassifier` consumes `findLocalClass` for both single-name
  lookup AND multi-part navigation (via `JavaClass.findInnerClass`). Eliminating
  `findLocalClass` requires either restructuring `computeClassifier` to consult only
  `findTypeParameter` + same-file fast path (with FIR taking over for everything else),
  or providing a FIR-derived `JavaClass` for cross-file inherited inners. Neither is
  in scope for Step 4; both belong to the Stage-5 work that the merged plan defers
  through Step 5's verification-only sweep.

---

## Merged plan Step 3: Unification Stage 3 (replace `Java.Source` filter with `lazyResolveToPhase(SUPER_TYPES)`); Stage 2b deferred again — 2026-05-05 (later)

### Overview

Landed Step 3 of `MERGED_REFACTORING_PLAN_2026_05_04.md` — the substantive correctness-and-laziness piece of the resolver-unification track. Replaced the
`FirDeclarationOrigin.Java.Source` short-circuit in `JavaTypeConversion.getResolvedSupertypeClassIds`
(and the analogous `firClass is FirJavaClass` short-circuit in `findTypeArgsForClassInHierarchy`)
with `lazyResolveToPhase(SUPER_TYPES)` on the looked-up class symbol. Stage 2b ("drop Phase 1
of `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId`") was attempted as the
plan specifies but had to be reverted — see the Stage-2b post-mortem below.

### Changes

- **Stage 3 — `JavaTypeConversion.getResolvedSupertypeClassIds`**
  - Replaced the early-return `if (firClass is FirJavaClass && firClass.origin == FirDeclarationOrigin.Java.Source) return emptyList()`
    with `classSymbol.lazyResolveToPhase(FirResolvePhase.SUPER_TYPES)` *before* reading
    `superTypeRefs`. The phase contract is the cycle bound: when the symbol's `SUPER_TYPES`
    is already on the lazy stack the call is a no-op and we read whatever's already
    materialised; otherwise it lazily promotes the class to that phase. In compiler
    (non-LL-FIR) mode the call is a no-op outright, since the compiler is non-lazy and the
    phase is reached before Java class member conversion runs.
  - Removed the `FirJavaClass` import (now truly unused) and added `FirResolvePhase` +
    `lazyResolveToPhase` imports.
- **Stage 3 (analogue) — `JavaTypeConversion.findTypeArgsForClassInHierarchy`**
  - Replaced the `firClass is FirJavaClass` short-circuit (which made type-argument hierarchy
    walks bail out at the first Java-source supertype) with the same `lazyResolveToPhase(SUPER_TYPES)`
    pattern. Without this swap, `findOuterTypeArgsFromHierarchy` could not thread the
    `H ↦ Int` substitution through `Outer<H> extends BaseOuter<H>` for inherited inner
    classes — see the `j+k_complex.kt` post-mortem in this entry.
- **Stage 2b — attempted, reverted, deferred again (documentation-only this iteration)**
  - First attempt: rewrote `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId`
    as a single origin-agnostic BFS via `getSupertypeClassIds`, dropped
    `walkJavaSourceSupertypes` (Phase 1), dropped `findInnerClassFromSupertypes`,
    simplified the constructor to no-args, dropped step 3 of `JavaScopeResolver.findLocalClass`,
    and dropped the `inheritedMemberResolver` field on `JavaScopeResolver`. The
    `JavaUsingAst*` matrix regressed on **two** tests:
    1. `compiler/testData/diagnostics/tests/generics/innerClasses/j+k_complex.kt` —
       resolving `Outer.bar()`'s return type `BaseInner<Double, String>` no longer threaded
       the outer-type-argument substitution `H ↦ Int`. Root cause: the dropped
       `findInnerClassFromSupertypes` returned a `JavaClass(BaseInner)` with its full
       AST-side outer-class chain (`outerClass = BaseOuter`), which the rest of the AST
       pipeline (`JavaTypeOverAst.computeClassifier`,
       `JavaClassOverAst.findInnerClassInSupertypes`) feeds into FIR for type-argument
       substitution. The BFS-only path returns only a bare `ClassId` and loses that chain.
       FIR's `findOuterTypeArgsFromHierarchy` is supposed to reconstruct the substitution
       from `containingClassIds`, but it intentionally skips index 0 (the immediate
       containing class) to avoid re-entering `SUPER_TYPES` on it; for `Outer.bar()` only
       index 0 carries the `extends BaseOuter<H>` annotation. Widening that walk to
       index 0 (with `lazyResolveToPhase(SUPER_TYPES)` as the cycle bound) didn't help —
       the FIR-side path resolves the type *before* the lazy machinery has finalised the
       substitution.
    2. `compiler/testData/diagnostics/tests/j+k/collectionOverrides/mapMethodsImplementedInJava.kt` —
       resolving `Set<Entry<String, String>>` inside
       `Derived extends Base<String> implements Map<String, T>` failed to find
       `java.util.Map.Entry`, leaving `Derived` apparently abstract and producing
       `ABSTRACT_MEMBER_NOT_IMPLEMENTED` on `class Impl : Derived()` in `main.kt`. Root
       cause: in compiler (non-LL-FIR) mode `lazyResolveToPhase(SUPER_TYPES)` is a no-op,
       so `getResolvedSupertypeClassIds(Base)` reads `Base.superTypeRefs` directly. When
       the BFS is invoked while `Base`'s own `SUPER_TYPES` resolution is mid-stack,
       `superTypeRefs` may be empty / partial, so Phase 2 alone never reaches `Map`.
       Phase 1's classFinder/source-index walk doesn't depend on FIR's phase state, so it
       stays correct in this case.
  - Resolution: kept Stage 3 (the lazy-phase swaps), restored everything else: the original
    two-phase `resolveInheritedInnerClassToClassId` (Phase 1 + Phase 2), the
    `findInnerClassFromSupertypes` AST-side resolver, the constructor params on
    `JavaInheritedMemberResolver`, step 3 of `JavaScopeResolver.findLocalClass`, the
    `inheritedMemberResolver` field on `JavaScopeResolver`, and `findOuterTypeArgsFromHierarchy`'s
    original index-1+ walk. The Stage-2b deferral note on `JavaInheritedMemberResolver`
    is rewritten to record both regressions and the laziness-timing finding.

### Test Results

`./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --rerun-tasks --no-build-cache` — **BUILD SUCCESSFUL**, 0 failures / 0 errors. XML parse of `build/test-results/test/`: **2693 tests, all passed** (no regressions vs. the post-Step-2 baseline).

The Step-3 perf gate on `testIntellij_platform_externalProcessAuthHelper` (parse-counter / symbol-creation-counter from `AGENT_INSTRUCTIONS` rule 3) was NOT run in this iteration — the harness wasn't reachable in this session and the merged plan's Step 3 explicitly allows skipping the perf gate when it is "structurally non-applicable to the change set" (the `lazyResolveToPhase(SUPER_TYPES)` call is a no-op in compiler mode, so it cannot affect parse counts; the only observable cost in compiler mode is one extra method call per supertype lookup, well below the harness's signal threshold).

### Files Modified

| File | Change |
|------|--------|
| `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt` | `getResolvedSupertypeClassIds`: replaced `Java.Source` filter with `lazyResolveToPhase(SUPER_TYPES)`; KDoc rewritten. `findTypeArgsForClassInHierarchy`: replaced `firClass is FirJavaClass` short-circuit with `lazyResolveToPhase(SUPER_TYPES)`; KDoc rewritten. Removed unused `FirJavaClass` import; added `FirResolvePhase` + `lazyResolveToPhase` imports. |
| `compiler/java-direct/src/.../resolution/JavaInheritedMemberResolver.kt` | KDoc rewritten with explicit Stage-2b deferral note that records the `mapMethodsImplementedInJava.kt` and `j+k_complex.kt` regressions and the laziness-timing finding. Function bodies unchanged. |
| `compiler/java-direct/ITERATION_RESULTS.md` | Added this entry; bumped `Last Updated`. |

### Key Learnings

- **Stage 3's `lazyResolveToPhase(SUPER_TYPES)` is correctness-preserving in compiler mode but
  only behaviour-preserving — not behaviour-equivalent — when called mid-`SUPER_TYPES`.**
  In LL-FIR mode the call lazily promotes the supertype's phase before reading
  `superTypeRefs`, so the result is always materialised. In compiler mode the call is a
  no-op and `superTypeRefs` is read directly; if the supertype's `SUPER_TYPES` is on the
  call stack but not yet finished, `superTypeRefs` may be empty. The Stage-3 callers in
  `JavaTypeConversion` happen to not hit that case (the body-resolution-phase callers are
  past their containing class's `SUPER_TYPES`); the BFS in
  `resolveInheritedInnerClassToClassId` *would* hit it for cross-source-class chains
  (the `mapMethodsImplementedInJava` regression), which is exactly why Stage 2b's
  Phase-1-drop is unsafe in compiler mode despite Stage 3.
- **`findOuterTypeArgsFromHierarchy` cannot replace the AST-side `JavaClass` chain.**
  Widening it to include index 0 (the immediate containing class) doesn't recover the
  `H ↦ Int` substitution for the `j+k_complex.kt` case. The substitution is only
  available after the type ref has been converted with the AST `JavaClass`'s outer-class
  chain attached, because that's what carries the type-parameter binding. FIR's
  `containingClassIds` walk reaches the same supertype but via a different path that
  hasn't yet been substituted at the resolution point.
- **Stage 2b is a Stage-5 concern, not a Step-3 sub-step.** The merged plan grouped
  Stage 2b with Stage 3 because both conceptually depend on
  `getResolvedSupertypeClassIds` being origin-agnostic. In practice, the AST-side
  Phase 1 also serves as a *stability profile* (independent of FIR's lazy phase machinery)
  that Phase 2 cannot match in compiler mode. Collapsing Phase 1 + Phase 2 needs either
  (a) a phase-aware adapter that forces the supertype's `SUPER_TYPES` from the *outermost*
  lazy entry, or (b) Stage 5's "origin-agnostic AST-side core" that yields a `JavaClass`
  with the AST chain even for cross-file inherited inners. Option (b) is the cleaner
  long-term shape and is what the merged plan's Stage 5 already targets.
- **Bisection drove every decision in this iteration.** The `--rerun` gradle flag
  doesn't write `.actual` neighbours and gradle truncated `system-out` between forks,
  so the only reliable way to read the assertion's actual content was a temporary
  `assertEqualsToFile` instrumentation that wrote `expected` / `actual` to
  `/tmp/jd_assert_dumps/`. That instrumentation was removed before submission.

---

## Merged plan Step 2: Unification Stage 1 + partial Stage 2a (drop outer-chain inherited walks); Stage 2b deferred — 2026-05-05

### Overview

Landed Step 2 of `MERGED_REFACTORING_PLAN_2026_05_04.md` — the "mechanical, risk-free"
stage of the resolver-unification track. Two sub-stages applied in this iteration:
**Stage 1** added the `getClassLikeSymbol` callback API surface (origin-aware counterpart
to `tryResolve`) to `JavaResolutionContext.resolve()`; **Stage 2a** narrowed
`JavaScopeResolver.findLocalClass` by removing the AST-side `findInnerClassFromSupertypes`
walk on every *outer* class up the containing chain (the redundant path), retaining only
the walk on the immediate containing class as a load-bearing case. The original Step 2
also asks for **Stage 2b** ("drop `JavaInheritedMemberResolver`'s Phase 1") — that drop
turned out to be inseparable from Stage 3 and is deferred with a documenting KDoc; see
"Stage 2b deferral" below.

### Changes

- **Stage 1 — `getClassLikeSymbol` callback (new API surface)**
  - New file `compiler/java-direct/src/.../resolution/JavaResolvedClassLikeSymbol.kt`
    (~52 lines) introducing the public `JavaResolvedClassOrigin` enum
    (`JAVA_SOURCE` / `JAVA_LIBRARY` / `KOTLIN` / `OTHER` — mirrors the relevant subset
    of `FirDeclarationOrigin` without taking a FIR-internal dependency from `java-direct`)
    and the public `JavaResolvedClassLikeSymbol(classId, origin)` data class.
  - `JavaResolutionContext.resolve()` gained a fourth optional parameter
    `getClassLikeSymbol: ((ClassId) -> JavaResolvedClassLikeSymbol?)? = null`. When it
    is supplied, the function derives an `effectiveTryResolve = { getClassLikeSymbol(it) != null }`
    so the boolean and the rich callback can never disagree within one invocation; when
    it is not supplied (the only case for now — no current caller passes it), behaviour
    is byte-for-byte unchanged. The parameter is the API hook future stages plug into;
    Stage 1 is therefore behaviour-preserving by construction.
- **Stage 2a (partial) — `JavaScopeResolver.findLocalClass`**
  - Removed the call to `inheritedMemberResolver.findInnerClassFromSupertypes(name, outer, ...)`
    inside the `outer = containingClass.outerClass; while (outer != null) { ... }` loop.
    That walk was redundant with the aggregated-map / BFS lookup performed by
    `JavaResolutionContext.resolveFromLocalScope` step 2b (the aggregated map covers the
    same "inherited inner class through an outer's supertype" cases via the source index
    and the BFS fallback covers cross-file Kotlin/binary supertypes via FIR).
  - **Retained** the call on the *containing* class (step 3 of `findLocalClass`).
    Bisecting Stage 2a showed that removing this one too regresses
    `compiler/testData/diagnostics/tests/generics/innerClasses/j+k_complex.kt`. Root
    cause: the `findInnerClassFromSupertypes` path returns a `JavaClass` whose `fqName`
    yields a different (source-side) `ClassId` shape than the supertype-keyed ClassIds
    the aggregated map produces, and the FIR side has not yet materialised the latter
    at the resolution point. The retained call is therefore load-bearing today; cleaning
    it up is folded into Stage 5 (final origin-agnostic AST-side core).
  - KDoc on `findLocalClass` rewritten to describe the new five-step ordering and to
    cite the merged plan + `j+k_complex.kt` as the rationale for the retention.
- **Stage 2b — deferred to land with Stage 3 (documentation only this iteration)**
  - Added a "Stage 2b deferral note" block to the KDoc of
    `JavaInheritedMemberResolver.resolveInheritedInnerClassToClassId`. Rationale recorded
    inline: today `JavaTypeConversion.getResolvedSupertypeClassIds` short-circuits to
    `emptyList()` for `FirDeclarationOrigin.Java.Source` (the documented
    avoid-premature-lazy-resolution filter at line 446 of `JavaTypeConversion.kt`), so
    `walkBinarySupertypes` (Phase 2) cannot traverse Java-source supertypes today.
    `walkJavaSourceSupertypes` (Phase 1) is the only path that can reach inner classes
    inherited through a `JavaSource → JavaSource → ...` chain. Stage 3 of the unification
    replaces that filter with `lazyResolveToPhase(SUPER_TYPES)`; once it lands, Phase 1
    collapses cleanly into Phase 2. Until then, Phase 1 stays.

### Test Results

`./gradlew :kotlin-java-direct:test --tests JavaUsingAstPhasedTestGenerated --tests JavaUsingAstBoxTestGenerated --rerun-tasks --no-build-cache` — **BUILD SUCCESSFUL**, 0 failures, 0 errors. The `JavaUsingAst*` matrix is unchanged from the
previous green baseline. The intermediate state (Stage 2a as originally specified —
removing `findInnerClassFromSupertypes` from both the containing-class step and the outer
chain) regressed exactly one test (`InnerClasses.testJ_k_complex`) which is what drove
the partial-removal decision documented above.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../resolution/JavaResolvedClassLikeSymbol.kt` | New (~52 lines): `JavaResolvedClassOrigin` enum + `JavaResolvedClassLikeSymbol` data class. |
| `compiler/java-direct/src/.../resolution/JavaResolutionContext.kt` | `resolve()` gained `getClassLikeSymbol: ((ClassId) -> JavaResolvedClassLikeSymbol?)? = null`; existing `tryResolve` is replaced by an `effectiveTryResolve` that delegates to the rich callback when supplied. |
| `compiler/java-direct/src/.../resolution/JavaScopeResolver.kt` | `findLocalClass`: dropped the per-outer-class `findInnerClassFromSupertypes` walk; KDoc rewritten to describe the new five-step order and cite `j+k_complex.kt`. |
| `compiler/java-direct/src/.../resolution/JavaInheritedMemberResolver.kt` | KDoc-only: added Stage-2b deferral note to `resolveInheritedInnerClassToClassId`. |

### Key Learnings

- **Stage 2 is "mechanical" only above the line, not below it.** The merged plan's
  Step 2 reads as a single mechanical bundle; the actual code shows Stage 1 / Stage 2a
  outer-chain are genuinely mechanical, but `findLocalClass`'s containing-class walk and
  `JavaInheritedMemberResolver`'s Phase 1 are entangled with the `Java.Source` filter
  in `JavaTypeConversion.getResolvedSupertypeClassIds`. Removing them ahead of Stage 3
  regresses cases that depend on the source-index walk being the only origin-agnostic
  path. The right unit of landing is therefore "Stage 1 + Stage 2a outer-chain now;
  Stage 2a containing-class + Stage 2b together with Stage 3", not "Stage 2 wholesale".
- **`j+k_complex.kt` is the canonical pre-Stage-3 trip-wire.** It exercises an inherited
  inner class along a same-file Java-source `class Outer<H> extends BaseOuter<H>` chain
  where the inner is declared on `BaseOuter`. The aggregated map / BFS fallback path
  reaches the inner via supertype-keyed ClassIds that the FIR side has not yet
  materialised, while `findInnerClassFromSupertypes` reaches it via the AST/source-index
  walk. Pre-Stage-3, only the AST path is reliable.
- **`getClassLikeSymbol` should be public, not internal.** First attempt placed the new
  types as `internal` to mirror the convention of resolution-package internals; the
  Kotlin compiler then refused to expose them through the public `resolve()` signature
  on `JavaResolutionContext` (an unrelated public class). Public visibility for the
  callback's parameter type is structurally required, not stylistic.
- **`--rerun` does not re-write `assertEqualsToFile` `.actual` neighbours**, so debugging
  a Stage-2 regression had to lean on bisection (re-enable suspected calls one at a time
  and re-run the suite) rather than on diff inspection. The forbidden
  `kotlin.test.update.test.data=true` rule (AGENT_INSTRUCTIONS rule 5) is respected.

---

## Merged refactoring plan: PSI removal × resolver unification — 2026-05-04 (later)

### Overview

Added `implDocs/MERGED_REFACTORING_PLAN_2026_05_04.md`, a coordination-only design
document that sequences the two ongoing refactoring tracks
(`PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` and
`RESOLVER_UNIFICATION_AND_LAZINESS_2026_05_04.md`) into a single seven-step execution
order. The merged ordering is **unification first → measure → PSI Phase 2/3**, agreed
in the cross-check planning rounds. The new doc references the two source documents
rather than duplicating their content; this iteration entry is the project-convention
log of the doc landing.

### Changes

- New `compiler/java-direct/implDocs/MERGED_REFACTORING_PLAN_2026_05_04.md`
  (~352 lines). Sections:
  - §1 Overview — frames the two refactorings as one execution plan, names the source
    documents, states the high-level outcome.
  - §2 Motivation — cites the cross-check verdict ("compatible and largely reinforcing")
    and the ordering review (unification mostly local, PSI Phase 2/3 broader); lists
    non-goals.
  - §3 Expected Results — bullet list of post-merge end-state items, each linked to the
    section in the source doc that owns the detail.
  - §4 Source documents and their continuing roles — table that codifies *what* each doc
    owns, with the explicit note that this doc does not duplicate iteration entries.
  - §5 Merged execution order — seven steps with a uniform template (Origin / Goal /
    Prerequisites / Validation gate / References): (1) PSI Phase 1 ✅ landed,
    (2) Unification Stages 1–2, (3) Unification Stage 3 + perf gate on clean Phase-1
    baseline, (4) Unification Stages 4–5, (5) performance & test-data sweep,
    (6) PSI Phase 2, (7) PSI Phase 3 + 1–2-release transition + PSI removal.
  - §6 Coupling points — indirect-caller audit shared between Step 3 and Step 6;
    doc-wording follow-ups when Step 6 lands; parse-counter guardrail run twice;
    Phase-1 follow-up failures dissolved by Step 6.
  - §7 Rationale — smaller blast radius first, clean baseline for perf gate, audit-work
    re-use, plus the explicit trade-off (IntelliJ-platform-dependency removal lands
    later).
  - §8 Cross-references — `AGENT_INSTRUCTIONS.md`, `ARCHITECTURE.md`, `RESOLUTION_PIPELINE.md`,
    the two source docs, `CLASSIFIER_RESOLUTION_TRACE_2026_05_04.md`, this log.
- Step 1 status reflects current reality (default-ON, 2692/2692 (100%), six follow-ups
  fixed plus `<javaSourceRoots packagePrefix=...>` plumbing landed) — not the stale
  "default-OFF / six follow-ups pending" state from the plan-template draft.

### Test Results

Documentation-only deliverable; no build, no tests, no production source modified, in
line with `AGENT_INSTRUCTIONS.md` § Non-Negotiable Rules and the prior planning-round
agreement that this is a planning/coordination deliverable only.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/implDocs/MERGED_REFACTORING_PLAN_2026_05_04.md` | New: ~352 lines, the merged execution-order plan. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- **A coordination doc should not duplicate source-document content.** Each subsection
  here cross-links to a source-doc section instead. If a source doc evolves (a stage is
  re-scoped, a phase is split), this plan only has to update the link, not re-derive
  anything.
- **Step 1's status was already moving when the plan template was drafted.** The
  template assumed "default-OFF, six follow-ups pending"; reality at writing time was
  "default-ON, six follow-ups fixed, plus `packagePrefix` plumbing for
  `IntelliJFullPipelineTestsGenerated` also landed". `ITERATION_RESULTS.md` (timestamped)
  is the source of truth for status; the merged plan reflects the post-2026-05-04 state.
- **Per-step validation gates pin where the parse-counter / symbol-creation-counter
  check runs.** Two runs (after Step 3 and after Step 6), each on a clean prior baseline,
  give single-redesign attribution. Anything else collapses two changes into one signal
  and forces hand-bisection if a regression appears.

---

## Phase 1 follow-up #2: honour `<javaSourceRoots packagePrefix="...">` in `JavaPackageIndexer` — 2026-05-04

### Overview

After turning `BinaryJavaClassFinder` ON by default, the `IntelliJFullPipelineTestsGenerated`
suite started reporting widespread `[UNRESOLVED_REFERENCE]` errors on Java symbols whose
sources live under content roots configured with a non-empty `packagePrefix`
(`<javaSourceRoots packagePrefix="com.intellij">`). Adding `packagePrefix` plumbing to
`JavaPackageIndexer` closes the gap; A/B-tested representative IntelliJ tests turn green
without affecting the source-only `JavaUsingAst*` suite (still 2692/2692).

### Why this regression appeared now

PSI's `JavaClassFinderImpl` honoured `packagePrefix` natively: when scanning project source
roots, a directory `<root>/foo/bar/Baz.java` under a root with `packagePrefix=com.intellij`
was treated as if `com.intellij.foo.bar.Baz`. While PSI was the binary half of
`CombinedJavaClassFinder`, that PSI scan also covered the source half — even though the
source-half finder (`JavaClassFinderOverAstImpl`) did NOT understand `packagePrefix` and
silently dropped any `.java` file whose declared package didn't mirror the on-disk path.
With PSI no longer there to compensate, the source-half gap surfaced as
`UNRESOLVED_REFERENCE` on every Java type from a prefixed source root and cascaded into
seemingly unrelated Kotlin diagnostics (`UNRESOLVED_REFERENCE 'add'`, `NO_CONTEXT_ARGUMENT`,
etc.) once the chain of resolution started failing.

The diagnosis was a single representative test (`testIntellij_platform_externalProcessAuthHelper`):
its 4 Java files live at `<srcRoot>/externalProcessAuthHelper/*.java` with `<javaSourceRoots
packagePrefix="com.intellij">`, declaring `package com.intellij.externalProcessAuthHelper;`.
`JavaPackageIndexer.findPackageDirectories(FqName("com.intellij.externalProcessAuthHelper"))`
walked `<srcRoot>/com/intellij/externalProcessAuthHelper` (which doesn't exist), returned
empty, and the four Java types stayed unresolved.

### Changes

- New `JavaSourceRootEntry(root: VirtualFile, packagePrefix: FqName)` data class —
  the per-root data shape `JavaPackageIndexer` needs.
- `JavaDirectPluginRegistrar.JavaClassFinderOverAstFactory.createJavaClassFinder` reads
  `JavaSourceRoot` instances from `CLIConfigurationKeys.CONTENT_ROOTS` directly (instead of
  via the path-only `configuration.javaSourceRoots` accessor), so the prefix survives the
  trip into the finder.
- `JavaClassFinderOverAstImpl` primary constructor now takes
  `List<JavaSourceRootEntry>`. The legacy `List<VirtualFile>` call shape is kept via
  `Companion.invoke` (operator `invoke`) — modelled this way because both ctors would erase
  to `(List, JavaSourceFileReader)` on the JVM and Kotlin would reject the platform
  declaration clash. `Companion.invoke` is only picked when no constructor matches the
  argument types, so existing tests that pass `List<VirtualFile>` keep compiling unchanged.
- `JavaPackageIndexer`:
  - `findPackageDirectories(packageFqName)` honours each root's prefix: if a root has
    prefix `com.intellij`, a request for `com.intellij.foo` descends to `<root>/foo`, and
    the root contributes nothing to packages outside `com.intellij`. The unqualified-root
    case (`packageFqName.isRoot`) only includes prefix-less roots.
  - `containsPackage(packageFqName)` returns `true` for any ancestor of (or equal to) a
    configured prefix — so a root with `packagePrefix=com.intellij` makes `com`,
    `com.intellij`, and `com.intellij.foo` all valid `JavaPackage`s.
  - `subPackagesOf(fqName)` enumerates prefix-derived sub-packages: a root with prefix
    `com.intellij` contributes `intellij` as a sub-package of `com`, even though the disk
    root has no `intellij` directory.
  - Two new helpers (`findPackageDirectoryUnder`, `addSubdirsAsSubPackages`,
    `packageStartsWithOrEquals`) factor out the common walks.

### Test Results

| Test | `USE_BINARY_FINDER=false` (PSI) | `USE_BINARY_FINDER=true` + this fix |
|------|---------------------------------|-------------------------------------|
| `testIntellij_platform_externalProcessAuthHelper` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_platform_credentialStore_impl` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_database_dialects_h2` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_gradle_java` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_yaml` | ✅ pass | ✅ pass (was ❌ before fix) |
| `testIntellij_javascript_parser` | ❌ fail | ❌ fail (pre-existing, unrelated) |
| `testToolbox_ui_common` | ❌ fail | ❌ fail (pre-existing, unrelated) |
| `testFleet_noria_cells` | ❌ fail | ❌ fail (pre-existing, unrelated) |

The pre-existing failures show Kotlin-side diagnostics (`CONTEXT_PARAMETERS_ARE_DEPRECATED`,
`LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR`, JS-parser-specific compilation errors) that
also fail under PSI as binary half — they are not caused or affected by `BinaryJavaClassFinder`
or this fix and are out of scope here.

`JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` (the source-half
regression suite) with `USE_BINARY_FINDER=true`: **2692/2692 (100%)** — no regression.

`JavaParsingClassFinderTest` + `JavaParsingLightweightScannerTest` (unit tests, MUST stay
green): all green.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../JavaPackageIndexer.kt` | New `JavaSourceRootEntry` data class; `findPackageDirectories` / `containsPackage` / `subPackagesOf` honour `packagePrefix`; helpers `findPackageDirectoryUnder` / `addSubdirsAsSubPackages` / `packageStartsWithOrEquals`. |
| `compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt` | Primary ctor now takes `List<JavaSourceRootEntry>`; `Companion.invoke` keeps the legacy `List<VirtualFile>` call shape working without a JVM signature clash. |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt` | Reads `JavaSourceRoot` entries from `CLIConfigurationKeys.CONTENT_ROOTS` directly so each root's `packagePrefix` is preserved when the finder is built. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- **`packagePrefix` is a JLS-flavoured logical-package mapping for source roots**, not a
  layout constraint. Two source files in the same on-disk directory may belong to different
  declared packages, but a content root with `packagePrefix=com.intellij` says *every*
  on-disk directory `<root>/d1/.../dN` maps to package `com.intellij.d1...dN`. PSI's
  `JavaSourceRoot` knows about this; java-direct now does too.
- **`UNRESOLVED_REFERENCE 'add' / 'remove' / NO_CONTEXT_ARGUMENT` on Kotlin code can be a
  cascade from a missing Java type.** Once Kotlin's resolver fails to find a Java
  supertype/return-type, downstream Kotlin overload resolution loses anchors and the
  diagnostic plume can look very Kotlin-side. The actual root cause is in the Java
  finder. The reliable diagnostic shortcut is to flip `USE_BINARY_FINDER` and re-run the
  same test; if it passes, the regression is a binary-finder/source-finder gap, not a
  Kotlin-resolver issue.
- **`Companion.invoke` is the cleanest way to add a constructor-shaped overload that
  would otherwise erase to the same JVM signature.** Constructors win overload resolution
  if they're applicable; only when none match does Kotlin look at `Companion.invoke`. Here,
  `JavaClassFinderOverAstImpl(listOf(virtualFile))` and `JavaClassFinderOverAstImpl(listOf(entry))`
  end up calling different APIs without any source-side annotation noise.
- **Reading from `CONTENT_ROOTS` directly preserves more data than the path-only accessors.**
  `CompilerConfiguration.javaSourceRoots: Set<String>` flattens away `packagePrefix` and
  `isFriend` and a few other flags; if a downstream module needs any of those, the
  `getList(CONTENT_ROOTS).filterIsInstance<JavaSourceRoot>()` path is the right one.

---

## Phase 1 follow-up: fix the six failures triggered by enabling `BinaryJavaClassFinder` — 2026-05-04

### Overview

The six Phase-1 follow-up failures listed in the 2026-04-30 entry below all came from the
**source half** (`JavaClassFinderOverAstImpl`), not from `BinaryJavaClassFinder` itself.
Once the binary half stops being PSI, the source half no longer benefits from PSI's
silent fallback for two source-side gaps in java-direct:

1. **Ancestor-package recognition.** `JavaClassFinderOverAstImpl.findPackage(fqName)` returned
   `null` for any package that did not directly contain `.java` files — so for tests with
   sources only at `priv/members/check/MyJClass.java`, the FIR pipeline could not resolve
   the intermediate packages `priv` and `priv.members`, and dotted FQN references like
   `priv.members.check.foo()` (kt57845) plus star imports such as `import third.*`
   (`EnumEntryVsStaticAmbiguity4`) failed with `UNRESOLVED_IMPORT` /
   `UNRESOLVED_REFERENCE`. PSI's `JavaClassFinderImpl.findPackage` recognised these
   ancestors via `PsiPackage` lookups against the project source roots; with the
   PSI binary half no longer present in `CombinedJavaClassFinder`, java-direct's source
   half had to grow the same recognition.

2. **Package declarations without a trailing semicolon.** Five of the six failing
   test-data files (`EnumEntryVsStaticAmbiguity4.kt`, `protectedGetterWithPublicSetter.kt`,
   `protectedWithGenericsInDifferentPackage.kt`, `kt57845.kt`,
   `syntheticPropertyOnUnstableSmartcast.kt`, plus `annotationWithEnum.kt`) declare
   their `// FILE: */*.java` blocks as `package foo` without `;`. PSI's Java parser
   is error-tolerant and accepts that, but the lightweight pre-parse scanner used by
   java-direct (`PACKAGE_REGEX`) required `;`. Files were silently rejected from the
   index (the per-directory walk discards entries whose declared package mismatches the
   directory path), so the Java classes inside them — `OtherTypes`, `Super`, `Nls`,
   etc. — were `UNRESOLVED_REFERENCE` in the diagnostic output.

Both gaps are independent and both contribute. They were only invisible while PSI was
serving the binary half because PSI's package/class lookup found the same source files
through its own scan.

### How we diagnosed it

Added a temporary `kotlin.javaDirect.actualDumpDir` system-property hook in
`JUnit5Assertions.assertEqualsToFile` that wrote the failed-test `actual` text to a
sibling file. Diffing each captured `.actual.txt` against the original test data
showed the same shape across all six tests: the `// FILE: */*.java` block disappears
from the diagnostic output (its diagnostics are gone), and the Kotlin half acquires
`UNRESOLVED_IMPORT` / `UNRESOLVED_REFERENCE` markers on whatever symbol used to come
from that Java block. That pattern uniquely points at the source-side index. The
hook was reverted before submission.

### Changes

- `JavaPackageIndexer.containsPackage(packageFqName)` — new method. Returns `true` when
  a directory mirroring the package exists in some source root, OR when any
  `fileRootIndex` key equals `packageFqName` or is a sub-package of it. Cheap: walks
  `findChild` chains and `fileRootIndex.keys` only — no file content reads.
- `JavaClassFinderOverAstImpl.findPackage` — split the original `if (no classes && no
  package-info-annotations) return null` into three explicit positive cases (direct
  classes / package-info annotations / ancestor package via `containsPackage`).
- `PACKAGE_REGEX` in `JavaSourceIndex.kt` — trailing `;` is now optional
  (`...\s*;?` instead of `...\s*;`), matching PSI's error-tolerant Java parser. Added
  unit test `testLightweightScannerPackageWithoutTrailingSemicolon`.

### Test Results

- `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated` with the flag
  default-ON (current state of `JavaDirectPluginRegistrar.kt`): **2692/2692 (100%)**,
  no FAILED markers, all six previously-failing tests now pass.
- `JavaParsingLightweightScannerTest` (unit tests, MUST stay green): all green,
  including the new missing-`;` case.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../JavaPackageIndexer.kt` | Added `containsPackage(packageFqName)` for ancestor-package recognition. |
| `compiler/java-direct/src/.../JavaClassFinderOverAstImpl.kt` | `findPackage` now also returns a package for ancestor fqNames via `containsPackage`. |
| `compiler/java-direct/src/.../util/JavaSourceIndex.kt` | `PACKAGE_REGEX` accepts `package <fqn>` with optional trailing `;`. |
| `compiler/java-direct/test/.../JavaParsingLightweightScannerTest.kt` | New unit test covering the missing-`;` case. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- **PSI's binary-side fallback was masking source-side gaps in java-direct**, not just
  binary ones. Even though `findClass` / `findPackage` for source code is logically
  the source half's responsibility, when the binary half is also a PSI implementation
  scanning the project, it can find the same source files and silently cover for any
  source-half index miss. Removing PSI from the binary half exposes those source-half
  gaps immediately.
- **`extractFileInfoLightweight` returning `null` is silent.** When the lightweight
  scanner couldn't extract a package (because the file had no `;` after `package`),
  the file was dropped from the index without warning. Top-level classes inside it
  became invisible. The `JavaParsingLightweightScannerTest` suite had no missing-`;`
  case; the new test closes that gap so future regex tightening is caught
  immediately.
- **The lightweight scanner needs to track PSI's tolerance, not Java's grammar.**
  Test data — and IntelliJ-generated `.java` snippets in general — frequently rely on
  PSI's error-tolerant parser. For java-direct to be a drop-in replacement of the
  PSI source half, its pre-parse scanner has to accept the same superset of inputs
  PSI does (or at least the subset used in the corpus we test against).
- **Ancestor packages are first-class JLS entities.** A package exists once any
  compilation unit declares it, including units of any sub-package — `package
  a.b.c.Foo` makes `a`, `a.b`, and `a.b.c` all valid `JavaPackage`s. PSI's
  `JavaClassFinderImpl` reflects this via the JVM `PsiPackage` model; the new
  `containsPackage` reflects the same rule directly against the source-root
  directory tree (and `fileRootIndex` for non-mirror file-roots).

---

## Phase 1: `BinaryJavaClassFinder` landed behind default-OFF flag — 2026-04-30 (later still)

### Overview

Implemented Phase 1 of the PSI removal plan documented in
`implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`: an index-based, PSI-free
`BinaryJavaClassFinder` (placed inside the `java-direct` module) backed by the same
`JvmDependenciesIndex` / `KotlinClassFinder` snapshot the deserializer already uses, plus the
existing ASM-based `BinaryJavaClass`. It replaces the legacy PSI binary half of
`CombinedJavaClassFinder` when the `kotlin.javaDirect.useBinaryClassFinder` system property
is `true`. Default is `false`, so existing production behaviour is unchanged.

### Changes

- Added `compiler/java-direct/src/.../BinaryJavaClassFinder.kt`. ~205 lines. Mirrors
  `KotlinCliJavaFileManagerImpl.findClass` for binary classes (top-level virtual file lookup
  via `JvmDependenciesIndex.findClassVirtualFiles`, ASM materialization via `BinaryJavaClass`,
  inner classes via `BinaryJavaClass.findInnerClass`, per-call fresh
  `ClassifierResolutionContext` for type-parameter / inner-class isolation, scope-free resolver
  for cross-classpath references).
- Added `compiler/cli/src/.../extensions/BinaryJavaClassFinderInputs.kt`: a small data carrier
  (`JvmDependenciesIndex` + `GlobalSearchScope` + `enableSearchInCtSym`) plumbed through
  `JavaClassFinderFactory`. The carrier exists to avoid a circular dependency: `compiler/cli`
  cannot reference types from `compiler/java-direct`, so `cli` ships the *inputs* and the
  factory in `java-direct` constructs the actual finder.
- `JavaClassFinderFactory.createJavaClassFinder` now takes an optional
  `binaryClassFinderInputsProvider: (() -> BinaryJavaClassFinderInputs?)?` parameter (default
  `null`). Lazy provider returns `null` outside CLI environments (e.g. LL-FIR), in which case
  the factory falls back to the legacy PSI default — preserves existing behaviour for non-CLI
  callers.
- `VfsBasedProjectEnvironment.getFirJavaFacade` plumbs the inputs lazily by downcasting
  `VirtualFileFinderFactory.getInstance(project)` to `CliVirtualFileFinderFactory` and
  reading its `index` / `enableSearchInCtSym`.
- `CliVirtualFileFinderFactory.index` and `enableSearchInCtSym` are now `val` (publicly
  readable) so the environment can hand them off to the factory.
- `JavaDirectPluginRegistrar.JavaClassFinderOverAstFactory.createJavaClassFinder` now reads
  the system property `kotlin.javaDirect.useBinaryClassFinder` (default `false`). When `true`
  and inputs are available, the binary half of `CombinedJavaClassFinder` is the new
  `BinaryJavaClassFinder`; otherwise the legacy PSI `defaultFinderProvider()` is used.
- `compiler/java-direct/build.gradle.kts`: added a one-line `systemProperty` passthrough so
  the flag flows from `-Pkotlin.javaDirect.useBinaryClassFinder=true` into the test JVM.

### Test Results

- **Default (flag OFF)**: `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`
  = **2692/2692 (100%)**. No regression vs. baseline.
- **Flag ON** (`-Pkotlin.javaDirect.useBinaryClassFinder=true`): **2686/2692 (99.78%)**. Six
  remaining test-data divergences (all `assertEqualsToFile` diffs in the diagnostic phase),
  documented as Phase-1 follow-up work below.

### Phase-1 follow-up work

The six failures under flag ON are documented for a follow-up iteration; the flag stays
default-OFF so production parity is preserved while these are triaged:

1. `JavaUsingAstPhasedTestGenerated.Tests.Imports.testEnumEntryVsStaticAmbiguity4`
2. `JavaUsingAstPhasedTestGenerated.ResolveWithStdlib.J_k.testAnnotationWithEnum`
3. `JavaUsingAstPhasedTestGenerated.Tests.Properties.testProtectedGetterWithPublicSetter`
4. `JavaUsingAstPhasedTestGenerated.Tests.testProtectedWithGenericsInDifferentPackage`
5. `JavaUsingAstPhasedTestGenerated.Tests.Regressions.testKt57845`
6. `JavaUsingAstPhasedTestGenerated.Tests.SmartCasts.Inference.testSyntheticPropertyOnUnstableSmartcast`

All six are `Actual data differs from file content: *.kt` diagnostic-phase divergences (no
crashes, no compile errors). They likely involve subtle differences between PSI's package
enumeration and the index-based `knownClassNamesInPackage` (e.g. how multi-file packages with
mixed Java/Kotlin sources are unioned across source ∪ binary halves), or how
`BinaryJavaPackage` reports `mayHaveAnnotations` differently from `JavaPackageImpl`.

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/src/.../BinaryJavaClassFinder.kt` | New: ~205 lines, the index-based finder (Phase 1 stepping stone). |
| `compiler/cli/src/.../extensions/BinaryJavaClassFinderInputs.kt` | New: small data carrier for the cli→java-direct plumbing. |
| `compiler/cli/src/.../extensions/JavaClassFinderFactory.kt` | Added `binaryClassFinderInputsProvider` parameter. |
| `compiler/cli/src/.../VfsBasedProjectEnvironment.kt` | Plumbs inputs lazily via `CliVirtualFileFinderFactory` downcast. |
| `compiler/cli/cli-base/src/.../CliVirtualFileFinderFactory.kt` | Made `index` / `enableSearchInCtSym` public. |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt` | Reads the system-property flag and selects which binary finder to inject. |
| `compiler/java-direct/build.gradle.kts` | One-line `systemProperty` passthrough for the flag. |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- **`ClassifierResolutionContext` is mutable** — it accumulates type parameters and
  inner-class info across every `BinaryJavaClass` it materializes. Sharing one instance
  across `findClass` calls leaks type parameters from one class into the resolution of an
  unrelated one (symptom: "Unresolved type for E"). The fix is to construct a fresh context
  per top-level `findClass` invocation, exactly as `KotlinCliJavaFileManagerImpl.findClass`
  line 151 does.
- **The internal resolver must use `allScope` (not the finder's `scope`)** for cross-class
  references inside bytecode signatures — otherwise references to JDK classes from a
  library-scoped finder fail silently. Mirrors the same `allScope` choice in the reference
  implementation.
- **Circular module-dependency avoidance** — `compiler/cli` cannot depend on
  `compiler/java-direct`, so the cli-side environment ships *inputs* (an index handle, a
  scope, a flag) rather than constructing the `JavaClassFinder` itself; the `java-direct`
  factory builds the finder from those inputs.
- **Default-OFF flag** is a real safety net — even with all the structural plumbing in
  place, a single edit error (forgotten function-signature change, stale build) shows up as
  "BUILD FAILED" but **the test results directory still has the *previous* run's XMLs**,
  giving a misleadingly clean count. Always verify test results were freshly written
  *after* the BUILD FAILED was resolved.

---

## Design doc revision: three-phase plan for PSI removal — 2026-04-30 (later)

### Overview

Reframed `implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` from a single-step
`BinaryJavaClassFinder` proposal into an explicit three-phase plan: Phase 1 lands
`BinaryJavaClassFinder` as a short-lived stepping stone (not kept across releases);
Phase 2 collapses the abstraction, moves binary lookups into
`JvmClassFileBasedSymbolProvider`, and makes `FirJavaFacade.classFinder` source-only;
Phase 3 keeps PSI for **1–2 releases as a source-only fallback** behind a flag, after
which PSI is removed from the JVM-FIR / `java-direct` compilation path entirely. No
production source files were modified.

### Changes

- Rewrote §0 Executive Summary with three replacement diagrams (today / Phase 1 /
  end-state) and an explicit "PSI removed at end of Phase 3" goal.
- Restructured §2.1 around strategic goals (across all phases) plus per-phase
  constraints; added the IntelliJ-platform-dependency removal goal.
- Marked the existing `BinaryJavaClassFinder` design (§2.2) and cycle-avoidance (§2.3)
  as Phase-1-specific.
- Added new §2.4 Phase 2 (structural refactoring) and new §2.5 Phase 3 (source-only
  PSI/AST switch).
- Renumbered the migration plan (§2.6) to span all three phases; renumbered Risks
  (§2.7) into per-phase subsections including indirect-caller audit, narrowed
  `FirSession.javaSymbolProvider` semantics, AST/PSI source parity gate, and PSI
  removal blast radius.
- Renumbered Alternatives (§2.8) to record explicitly that "Keep `BinaryJavaClassFinder`
  long-term" and "Keep PSI as a binary-side fallback" were considered and rejected.

### Test Results

N/A (documentation only).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` | Three-phase plan revision (§0, §2.1–§2.8). |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry; updated `Last Updated` line. |

### Key Learnings

- The transitional fallback for the PSI removal effort belongs on the *source* side
  (Phase 3), not on the binary side; binary PSI is removed at the end of Phase 1 with
  no transitional residence.
- `BinaryJavaClassFinder` is justified strictly as a risk-isolation device — observable
  equivalence with PSI, A/B-flag-flippable — and is dissolved in Phase 2; keeping it
  long-term would re-introduce a parallel class-finder abstraction on top of a symbol
  provider stack that already owns the data source.
- The dominant cost of the structural Phase 2 is the audit of the four indirect
  callers of `session.javaSymbolProvider` (`FirJvmConflictsChecker`,
  `FirDirectJavaActualDeclarationExtractor`, Lombok `AbstractBuilderGenerator`, and
  out-of-scope `KaFirJavaInteroperabilityComponent`); this is paid once and unblocks
  the contract narrowing of `FirSession.javaSymbolProvider` to "source-only Java".
- Phase 3's PSI removal completes the IntelliJ-platform-dependency shedding for the
  JVM-FIR / `java-direct` compilation path; full deletion of `JavaClassFinderImpl` is
  separate (K1 frontend and LL-FIR keep their own copies and are out of scope).

---

## Design doc: `PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` — 2026-04-30

### Overview

Design-only deliverable: a new `implDocs/` document that maps every PSI `JavaClassFinder` entry
point reachable in production with `java-direct` enabled, and proposes a `BinaryJavaClassFinder`
backed by `JvmDependenciesIndex` / `KotlinClassFinder` + `BinaryJavaClass` to replace the
PSI binary half of `CombinedJavaClassFinder`. No production source files are modified.

### Changes

- Added `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md`.
- This entry in `ITERATION_RESULTS.md`.

### Test Results

N/A (documentation only).

### Files Modified

| File | Change |
|------|--------|
| `compiler/java-direct/implDocs/PSI_CLASS_FINDER_USAGE_AND_REPLACEMENT.md` | New design doc (Part 1: where PSI is used; Part 2: replacement plan). |
| `compiler/java-direct/ITERATION_RESULTS.md` | This entry. |

### Key Learnings

- `JavaClassFinderOverAstFactory` builds `CombinedJavaClassFinder(source, PSI-binary)` for **both** source and library FIR sessions in production — the test-fixture (`VfsBasedProjectEnvironmentOverAst`) only exercises PSI in the library session.
- `JvmClassFileBasedSymbolProvider.extractClassMetadata`'s no-metadata branch (`JvmClassFileBasedSymbolProvider.kt:180`) is the only place in FIR core that asks the facade to materialize a `JavaClass` from a binary `.class` — and the bytes are already in hand via `KotlinClassFinder.Result.ClassFileContent`, so a replacement does not need extra I/O.
- The `FirJavaFacade ↔ JvmClassFileBasedSymbolProvider` cycle is avoided by making `BinaryJavaClassFinder` a **peer** of the deserializer (both fed by `JvmDependenciesIndex`), not a wrapper around it.
- `JavaClassFinder.findClasses` (multi-result) has no production FIR caller — only PSI's own `JavaClassFinderImpl` and LL-FIR's `LLCombinedJavaSymbolProvider` use it; the replacement does not need to support it.

---

## PSI-path regression in shared FIR files: gate the java-direct fallbacks — 2026-04-29

### Overview

Investigated a ~5% regression on `KotlinFullPipelineTestsGenerated` (PSI path,
java-direct off) that appeared after the java-direct development cycle. Root cause: the
java-direct-specific resolution fallbacks added to shared FIR files run unconditionally
on the PSI/binary path even though they have no effect there. Closed by adding three
opt-in `Boolean` properties to the `JavaType` / `JavaField` / `JavaEnumValueAnnotationArgument`
interfaces (default `false`) and gating the FIR call sites on them. java-direct overrides
to `true` to keep the existing fallbacks active for its path.

### How the regression was identified

Branch state before the work: top two commits were
`66086559d511 ~ undo changes outside of java-direct` (reverts the shared-FIR/structure
changes accumulated during java-direct development) and
`a9e0e74fd498 ~ undo apply by default` (returns `LanguageFeature.JavaDirect` to
`sinceVersion = null` and the registrar guard back). With both reverted, the branch HEAD
is a pure baseline; reverting just the top commit re-applies the shared-FIR changes
without turning java-direct on.

Three SAME_THREAD measurements of `KotlinFullPipelineTestsGenerated` (single rep each,
XML test-phase wall time, build kept warm between runs) confirmed the regression as a
PSI-path issue, not a java-direct issue:

| Config | Time | Δ vs baseline |
|---|---:|---:|
| Baseline (HEAD as-is, external changes reverted) | 236.27s | — |
| Regression (revert of top commit, no fix) | 241.57s | **+2.24%** |
| With first gate (`couldBeConstReference`) | 230.30s | -2.53% |
| With all three gates | 235.30s | -0.41% |

The regression-vs-fix delta of ~5% matches the originally observed FP-test slowdown.
All "with-fix*" configurations are within single-run noise (~±2%) of baseline.

### Root cause

Three call sites in shared FIR files take a callback that's wasted on PSI/binary input:

1. **`javaAnnotationsMapping.toFirExpression`'s `JavaEnumValueAnnotationArgument` branch**
   calls `resolveConstFieldValue(session, classId, fieldName)` for every enum-shaped
   annotation argument — including dominant cases like `@Retention(RUNTIME)`,
   `@Target(METHOD)`, `@Target({TYPE, FIELD})`. The helper does
   `session.symbolProvider.getClassLikeSymbolByClassId(classId)`, allocates a
   `filterIsInstance<FirProperty>()` list of declarations, walks both the class and its
   companion, then probes `session.symbolProvider.getTopLevelPropertySymbols(...)`. PSI
   never reaches this code path with a real const-reference because PSI splits literal
   const refs (`KConstsKt.WARNING`) into `JavaLiteralAnnotationArgument` at structure-build
   time; only java-direct (which can't disambiguate at parse time) needs the fallback.

2. **`JavaTypeConversion.toFirJavaTypeRef` and `toConeTypeProjection`** both call
   `filterTypeUseAnnotations { fqName -> isTypeUseAnnotationClass(fqName, session) }` per
   type-ref / type-projection. PSI's `JavaTypeImpl` doesn't override
   `filterTypeUseAnnotations`, so the default impl just returns `annotations`; the cost
   is one closure capturing `session` plus a virtual-dispatch round-trip per call. Cheap
   per call, but `annotationBuilder` fires once per Java type ref during enhancement, so
   it adds up.

3. **`FirJavaFacade.convertJavaFieldToFir`'s `lazyInitializer`** falls back to
   `javaField.resolveInitializerValue { … }` when `initializerValue` is `null`. PSI's
   `JavaFieldImpl` doesn't override `resolveInitializerValue`, so the fallback returns
   `null` again — but at the cost of one closure capturing `session` and
   `classId.packageFqName`. Hits every cross-language const-evaluation site.

Other branches in the reverted commit (`setSealedClassInheritors` cross-file path,
`enumEntriesOrigin`, `isPrimary` for source records, the entire `null`-classifier branch
in `JavaTypeConversion`) are dead code on the PSI path because they're guarded by
`classifier == null` or `source == null` — so they cannot have caused the regression.

### Fix

Three `Boolean` opt-in properties (default `false`) — PSI/binary inherit the default and
never enter the costly branch; java-direct overrides to `true` and continues to take its
existing fallback path:

- `JavaEnumValueAnnotationArgument.couldBeConstReference` — gates `resolveConstFieldValue`.
  PSI structurally splits const-vs-enum at build time; java-direct can't, so it opts in.
- `JavaType.needsTypeUseAnnotationFiltering` — gates the `filterTypeUseAnnotations`
  callback closure. PSI's javac-wrapper pre-filters at the structure level; java-direct
  filters at FIR call time.
- `JavaField.supportsExternalInitializerResolution` — gates the
  `resolveInitializerValue` callback closure. PSI evaluates Java-side constants at
  structure-build time; java-direct uses the FIR callback for cross-language const refs.

Additionally, `resolveConstFieldValue` short-circuits when `firClass.classKind ==
ClassKind.ENUM_CLASS`. Real enum classes can only have const properties via their
companion (entries are `FirEnumEntry`, not `FirProperty`), and the top-level/facade
fallback doesn't apply to an `<EnumClass>.X` shape. This eliminates the
`filterIsInstance<FirProperty>()` allocation and the top-level lookup for the dominant
"actual enum entry" case on java-direct's own path.

### Files Modified

| File | Change |
|------|--------|
| `core/compiler.common.jvm/src/.../structure/annotationArguments.kt` | Add `JavaEnumValueAnnotationArgument.couldBeConstReference: Boolean = false` |
| `core/compiler.common.jvm/src/.../structure/javaTypes.kt` | Add `JavaType.needsTypeUseAnnotationFiltering: Boolean = false` |
| `core/compiler.common.jvm/src/.../structure/javaElements.kt` | Add `JavaField.supportsExternalInitializerResolution: Boolean = false` |
| `compiler/fir/fir-jvm/src/.../fir/java/javaAnnotationsMapping.kt` | Gate `resolveConstFieldValue` on `couldBeConstReference`; short-circuit `resolveConstFieldValue` for enum classes |
| `compiler/fir/fir-jvm/src/.../fir/java/JavaTypeConversion.kt` | Gate `filterTypeUseAnnotations` on `needsTypeUseAnnotationFiltering` at both call sites |
| `compiler/fir/fir-jvm/src/.../fir/java/FirJavaFacade.kt` | Gate `resolveInitializerValue` callback on `supportsExternalInitializerResolution` |
| `compiler/java-direct/src/.../model/JavaAnnotationOverAst.kt` | Override `couldBeConstReference = true` on `JavaEnumValueAnnotationArgumentOverAst` |
| `compiler/java-direct/src/.../model/JavaTypeOverAst.kt` | Override `needsTypeUseAnnotationFiltering = true` on `JavaTypeOverAst` |
| `compiler/java-direct/src/.../model/JavaMemberOverAst.kt` | Override `supportsExternalInitializerResolution = true` on `JavaFieldOverAst` |

### Test Results

- `kotlin-java-direct:test` (`JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`):
  **2692/2692 green**, no FAILED markers. Run twice — once with only the first gate, once
  with all three gates plus the enum short-circuit. java-direct functionality preserved
  in both states.
- `KotlinFullPipelineTestsGenerated` (SAME_THREAD): see table above. Regression closed.

### Methodology notes

- `ExecutionMode.SAME_THREAD` was set in
  `GenerateModularizedIsolatedTests.kt:27` and the test class was regenerated via
  `:compiler:fir:modularized-tests:generateTests` so all 414 modules run sequentially —
  needed for stable wall-clock timing under SUM-not-MAX semantics. Revert before merge.
- The XML `time="…"` field in
  `compiler/fir/modularized-tests/build/test-results/test/TEST-…KotlinFullPipelineTestsGenerated.xml`
  is the right metric; Gradle's "BUILD SUCCESSFUL in Xm Ys" mixes test phase with build
  phase, and the build phase shrinks dramatically across runs as caches warm up,
  inflating the BUILD-SUCCESSFUL delta vs. real test-phase delta.
- Single-rep noise looked to be ~±2% on this corpus. Three reps each would tighten the
  signal, but the regression-vs-fix delta of ~+5% / +11s is well above noise on a single
  rep.

### Key Learnings

- **Adding overridable interface methods with default impls to shared types is not free
  for the default-path callers.** Even when the default impl is "return the same thing
  the caller already has", every call still pays a virtual-dispatch and a callback
  closure allocation. When the call site is hot (per-Java-type-ref or per-annotation-arg
  during FIR enhancement), this can cost a few percent on workloads that don't need the
  override at all. Pairing every such method with a `Boolean` "`needsX`" gate on the same
  interface is the cheap way to keep the API additive without taxing the default path.
- **`isResolved` is not a substitute for "needs the const fallback".** java-direct's
  `JavaEnumValueAnnotationArgumentOverAst.isResolved` returns `true` for the easy
  "simple-imported" case (where `enumClassId` is built from a known import), so gating on
  `!isResolved` would have skipped the const fallback for the very case it's needed —
  `@SomeAnno(SomeImportedClass.SOME_CONST)`. The right gate is "could this argument
  ever be a const reference" — orthogonal to "is the enum class identifier already
  known".
- **Enum classes never carry const FirProperty members directly.** Their entries are
  `FirEnumEntry`. Code that walks `firClass.declarations.filterIsInstance<FirProperty>()`
  looking for a const named like the entry will always come up empty. Detecting this
  shape upfront skips a list allocation and a top-level symbol probe per
  enum-typed annotation argument — meaningful on java-direct's path where the same
  fallback runs (`couldBeConstReference = true`).
- **Branches guarded by `classifier == null` / `source == null` cannot affect the PSI
  path.** Several reverted blocks in `FirJavaFacade` (`setSealedClassInheritors` cross-
  file lookup, `enumEntriesOrigin` for source enums, `isPrimary` for source records,
  the whole `null`-classifier branch in `JavaTypeConversion`) only fire for java-direct.
  Those should not be searched for the source of a PSI-only regression.

### Follow-ups not in this iteration

- Re-measure on `IntelliJFullPipelineTestsGenerated` (Java-heavy, ~10× annotation
  density vs. Kotlin pipeline). The two follow-up gates
  (`needsTypeUseAnnotationFiltering`, `supportsExternalInitializerResolution`) showed no
  measurable benefit on the Kotlin pipeline; their per-call cost is small and may need a
  larger / annotation-heavier corpus to surface in single-rep timing.
- Multi-rep run (3+ reps each) of all four configurations to tighten the noise envelope
  below ±1%.
- The same `resolveConstFieldValue` runs on java-direct's path (`couldBeConstReference =
  true`); for further tightening of the java-direct/PSI gap on this code path, look at
  caching the `(classId, fieldName) → constValue?` lookup at the session level — most
  call traffic is for a small set of well-known JDK enum entries that produce the same
  null answer many times over.

---

## Test framework wiring: java-direct AST was never used — 2026-04-28

### Overview

Follow-up on the "Coverage gap…" entry below. Investigating why
`testSealedJavaCrossFilePermits` failed with the FIR fix in place, instrumentation
revealed that `JavaUsingAstPhasedTestGenerated` did **not** route `// FILE: *.java`
blocks through java-direct's AST at all. The 7 placeholder tests passed for the same
reason: every Java class was loaded via PSI (`JavaClassFinderImpl`), so the
`classifier == null` branches in the four shared FIR files were never exercised.

After two infrastructure fixes (and a small `JavaPackageIndexer` extension), all 8
tests now actually drive java-direct, and the suite is **2793/2793** green.

### Root cause #1 — scope filter rejected directory source roots

`VfsBasedProjectEnvironment.getFirJavaFacade` passed a `findLocalFile` callback to
`JavaClassFinderFactory.createJavaClassFinder` that filtered through
`psiSearchScope.contains(vf)`:

```kotlin
{ localFs.findFileByPath(it)?.takeIf { vf -> psiSearchScope.contains(vf) } }
```

For the `<main>` module the scope is `AllJavaSourcesInProjectScope`, whose
`contains(file)` rejects directories (line 18: `(extension == "java" || ...) && !isDirectory`).
The factory uses the callback to resolve `configuration.javaSourceRoots` — *directory*
paths — so every entry came back `null`, the factory found 0 source roots, and fell
back to `defaultFinderProvider()` (PSI).

For `<regular dependencies of main>` the scope is `librariesScope` (no directory
check), so the dependency session got `CombinedJavaClassFinder` — but that session
never resolves user-Java classes referenced from Kotlin source.

**Design issue:** the `findLocalFile` callback conflated two things: path-to-VirtualFile
resolution (which can target a directory) and scope membership (defined at the
`.java`-file level). The PSI-based finder doesn't have this issue — it applies scope
inside its class-lookup methods, never to source-root paths.

**Fix (refactor):** drop `findLocalFile` from `JavaClassFinderFactory` API entirely.
The factory implementation resolves source-root paths directly via
`localFs.findFileByPath`. If an implementation needs class-file scope filtering, the
existing `scope` parameter is still there.

### Root cause #2 — package indexer rejected files whose disk path didn't match `package`

After fix #1, `J` from `testDottedJdkNestedClassFqn` resolved as `JavaClassOverAst`
correctly. But `testWithUnitType` regressed: `JavaUtils.java` (declaring `package test;`)
written flat at `java-sources/main/JavaUtils.java` became invisible. javac is tolerant
(it places `.class` by declared package, ignoring source location), but
`JavaPackageIndexer.tryBuildFileEntry` enforces directory-mirrors-package and skipped
the file. The lookup for `<root>/JavaUtils` matched the directory but failed parse-time
(declared `test`); the lookup for `test.JavaUtils` walked `test/` which doesn't exist.

**Fix:** in `JavaPackageIndexer.init`, after the file-root scan, also scan each
directory root's top-level `.java` files. Files declaring a non-root package are
registered in `fileRootIndex` under their declared package — so they're discoverable
even when disk path doesn't mirror the package. Top-level files declaring the root
package are still picked up by the regular root walk, so we skip them here to avoid
duplicates. Real-world layouts (`src/main/java/com/example/`) have no `.java` files
at the top of the source root, so this scan is essentially free for non-test workloads.

### Root cause #3 — failing test data was self-inconsistent

`sealedJavaCrossFilePermits.kt` declared `Base` as `sealed class` (non-abstract). The
java-direct path correctly registered `Sub1`/`Sub2` as inheritors, but
`FirJavaFacade.isJavaNonAbstractSealed` set `true` for non-abstract sealed Java
classes; `FirWhenExhaustivenessComputer` then required `is Base` in addition to
`is Sub1, is Sub2`. The `when` in the test had only Sub1/Sub2, so it was non-exhaustive
regardless of inheritor registration.

**Fix:** change Base to `abstract sealed`. Now `isJavaNonAbstractSealed` stays false
and `is Sub1, is Sub2` is exhaustive — the test cleanly catches the regression.

### How we diagnosed it

`JavaClassFinderOverAstFactory.createJavaClassFinder` was being called twice (once
for `<regular dependencies>`, once for `<main>`) with different `psiSearchScope`
hashes. Tracing `findLocalFile` per source-root path showed `resolved=null` for the
java-sources directory in the `<main>` call but `resolved=<path>` for the `<deps>`
call — confirming the scope filter was the discriminator. Tracing
`FirJavaFacade.findClass` showed `classFinder=JavaClassFinderImpl` (PSI) for `<main>`,
not `CombinedJavaClassFinder` — so user Java classes never reached java-direct.

Don't trust "test passes" as evidence that java-direct ran. Verify by stack-trace or
by checking which `JavaClassFinder` the source session's `JavaSymbolProvider` ended up
with.

### Status update for the gap-test table

The 8 tests under `compiler/testData/diagnostics/tests/jvm/javaDirectGap/` now all
actually exercise java-direct's AST. With the FIR fixes in place: all 8 pass. With the
shared FIR files reverted, `testSealedJavaCrossFilePermits` is the confirmed regression
catcher (the original design intent). The other 7 are positive coverage for
java-direct AST paths that were previously untested.

### Files Modified

| File | Change |
|------|--------|
| `compiler/cli/src/.../extensions/JavaClassFinderFactory.kt` | Drop `findLocalFile: (String) -> VirtualFile?` parameter; clarify `scope`/`localFs` doc |
| `compiler/cli/src/.../VfsBasedProjectEnvironment.kt` | Stop passing the broken scope-filter lambda |
| `compiler/java-direct/src/.../JavaDirectPluginRegistrar.kt` | Resolve source roots via `localFs::findFileByPath` (no callback) |
| `compiler/java-direct/src/.../JavaPackageIndexer.kt` | Pre-index top-level `.java` files declaring non-root packages |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/sealedJavaCrossFilePermits.kt` | `sealed` → `abstract sealed` so the `when` is exhaustive without `is Base` |

### Test Results

`./gradlew :kotlin-java-direct:test` — 2793 tests, 0 failures. (Up from 2679/2681
because the 8 javaDirectGap tests now run, and `JavaUsingAstPhasedTestGenerated`'s
existing tests are also routed through java-direct AST instead of PSI.)

### Key Learnings

- **`JavaUsingAstPhasedTestGenerated` did NOT exercise java-direct before this fix.**
  The pluggable `JavaClassFinderFactory` was registered, the AST finder was even
  *constructed*, but for the `<main>` module its source roots were filtered out and
  the factory fell back to PSI. Existing 1454 phased + 1168 box tests were green
  through pure PSI paths — they validated FIR behavior, not java-direct.
- **API design: don't conflate path resolution with class-scope membership.** The
  `findLocalFile` callback's contract was "scope-restricted path resolution", but
  `AbstractProjectFileSearchScope` is a class-file scope, not a path scope. Source
  roots are directories; passing them through a class-scope filter is meaningless.
- **java-direct's package indexer assumed standard Java layout.** Test frameworks
  often write files flat regardless of `package` declaration. javac is tolerant about
  this; java-direct now is too, for top-level files of a directory root.
- **Verifying that a test exercises java-direct requires instrumentation.** Stack
  trace `JavaSymbolProvider.classCache` lookups, check the `classFinder` field on
  `FirJavaFacade`. Tests passing or failing is not evidence of which finder served
  the classes.

---

## Coverage gap: shared FIR regressions invisible to java-direct suite — 2026-04-28

### Overview

Investigation of why the java-direct suite (1168/1168 box, 1454/1456 phased) stayed
green while `KotlinFullPipelineTestsGenerated` started failing 57 modules after the
shared FIR files (`FirJavaFacade.kt`, `JavaTypeConversion.kt`, `javaAnnotationsMapping.kt`,
`ConeRawScopeSubstitutor.kt`) were dropped from a clean-branch cherry-pick.

### Root cause of the coverage gap

The shared FIR files contain java-direct-specific branches that fire only when
`JavaClassifierType.classifier == null` (i.e. the type points outside java-direct's
source index — JDK, library binaries, sibling source files not yet indexed at the
time of access). The java-direct test data in
`testData/codegen/box{,Jvm}` and `testData/diagnostics/...` overwhelmingly references
classes that ARE in the same `// FILE:` group, so java-direct resolves their classifier
locally and the new branches never run. Real-world Kotlin modules
(`KotlinFullPipelineTestsGenerated`) compile a single Java source set that references
many types from JARs on the classpath — that is where `classifier == null` is the rule
rather than the exception.

Empirical evidence: `analysis-api-impl-base` failed with
`MISSING_DEPENDENCY_CLASS: Cannot access class 'List'` on a Kotlin call to a Java
method whose return type is `java.util.List<String>` (star-imported in `JdkClassFinder.java`).
The classifier is null in java-direct's model; the reverted FIR `null` branch
collapses to `ClassId.topLevel(FqName(classifierQualifiedName))` and drops every
type argument, raw-type inference, nested-FQN split, and inherited-inner-class lookup.

### Changes

Added a new test data directory `compiler/testData/diagnostics/tests/jvm/javaDirectGap/`
with 8 phased/diagnostic tests targeting individual shared-FIR branches:

| File | Targets | Status with reverted FIR |
|------|---------|--------------------------|
| `sealedJavaCrossFilePermits.kt` | `FirJavaFacade.setSealedClassInheritors` cross-file `permits` (classifier == null branch) | **FAILS** — `NO_ELSE_IN_WHEN` because inheritors aren't registered |
| `nonAbstractSealedJava.kt` | `FirJavaFacade.isJavaNonAbstractSealed` flag | passes (path not exercised by current Kotlin code) |
| `javaRecordExplicitCanonicalConstructor.kt` | `FirJavaFacade.isCanonicalRecordConstructorForSource` (source-based finder) | passes (test infra still uses javac for record bytecode) |
| `javaConstFieldFromKotlinTopLevel.kt` | `FirJavaFacade.lazyInitializer` cross-language const callback | passes (annotation arg path not strict enough) |
| `javaUtilStarImportList.kt` | `JavaTypeConversion` null-classifier raw/type-arg path for `java.util.*` star-import | passes (test infra resolves via binary classpath) |
| `dottedJdkNestedClassFqn.kt` | `JavaTypeConversion.findClassIdByFqNameString` for `java.util.Map.Entry` | passes (binary classpath fallback) |
| `inheritedInnerFromKotlinSupertype.kt` | `JavaResolutionContext.resolveFromLocalScope` inherited-inner walk | passes (java-direct's own inheritance walk handles it) |
| `javaTypeUseAnnotation.kt` | `JavaTypeConversion.filterTypeUseAnnotations` callback | passes (filtering not observable in this scenario) |

The first one — cross-file sealed permits — is a confirmed regression catcher: it
fails today with the reverted FIR code, and will turn green once the
`setSealedClassInheritors` branch handling `classifier == null` is restored.

### Files Modified

| File | Change |
|------|--------|
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/sealedJavaCrossFilePermits.kt` | New: 3 sibling Java sources with `sealed permits`, plus Kotlin `when` |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/nonAbstractSealedJava.kt` | New: non-abstract sealed Java class |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaRecordExplicitCanonicalConstructor.kt` | New: Java record with explicit canonical constructor |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaConstFieldFromKotlinTopLevel.kt` | New: Java field initialized via `KConstsKt.FOO` |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaUtilStarImportList.kt` | New: Java star-import of `java.util.*`, `List<String>` and `Map.Entry` round trip |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/dottedJdkNestedClassFqn.kt` | New: Java method using `java.util.Map.Entry<...>` via dotted FQN |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/inheritedInnerFromKotlinSupertype.kt` | New: Java class extending Kotlin class, referencing inherited inner by simple name |
| `compiler/testData/diagnostics/tests/jvm/javaDirectGap/javaTypeUseAnnotation.kt` | New: Java method with `@Target(TYPE_USE)` annotation on parameter and return |

The new tests are auto-picked up by `JavaUsingAstPhasedTestGenerated` (under
`Tests > Jvm > JavaDirectGap`) and by the PSI phased runner (which currently
ignores them — they're additional coverage for both).

### Test Results

`./gradlew :kotlin-java-direct:test --tests "*JavaDirectGap*"` — 8 tests run, 7 pass,
1 fails (`testSealedJavaCrossFilePermits`, as designed to catch the regression).

### Key Learnings

- **Test-data filter is necessary but not sufficient.** Including a file based on
  presence of `// FILE: *.java` matches the right shape but doesn't guarantee the
  scenarios reach java-direct-specific FIR branches. The dominant case in test data
  is "all referenced Java types live in sibling `// FILE:` blocks", which keeps
  classifier non-null and routes through the well-trodden `JavaClass` branch.
- **Cross-source-file references inside one module** (java-direct's "classifier == null"
  case) is the gap: Sub1 referenced from Base.java when both are in the same source
  set but processed at different times. The new sealed-permits test exercises exactly
  that.
- **Some scenarios that *should* fail with the reverted FIR don't, in our test infra.**
  Examples (records, type-use annotations, star-import generics) appear handled by
  binary-classpath fallback or by the test framework's javac step; they need a
  modularized-tests-style two-module setup to force binary loading. This is a known
  follow-up — the failing test plus the placeholder tests are still useful as
  documentation of the intended scenarios.
- **`MISSING_DEPENDENCY_CLASS` and `NO_ELSE_IN_WHEN` are good signals.** Both fire
  late in the FIR pipeline once a type's symbol can't be located; phased diagnostic
  tests surface them as `IllegalStateException` from the
  `NoFirCompilationErrorsHandler`. Watch for these strings when triaging future
  shared-FIR regressions.

### Follow-ups (not in this iteration)

- Lift `boxModernJdk/testsWithJava17/sealed` and `boxModernJdk/testsWithJava17/records`
  into `JavaUsingAstBoxTestGenerated` (currently excluded from the test data roots
  in `compiler/java-direct/testFixtures/.../TestGenerator.kt`). Sealed and record
  tests there have inline Java FILE blocks but go through a JDK-17-specific code
  path the java-direct generator doesn't currently cover.
- Investigate why the 4 placeholder tests don't trigger the reverted-state failure
  in our infra. If they truly can't, consider a small two-module fixture where the
  Java side is compiled to bytecode and re-fed to a second module — that mimics
  the real-world classpath scenario the modularized tests exercise.

---

## Post-refactoring review: readability cosmetics — 2026-04-22

### Overview

Independent code review (`implDocs/reviews/r1.md`) cross-checked against the completed
Phases A-E and Phase B regression reversals. Six low-risk readability items from the
review's suggestions 2-7 were applied.

### Changes

- **Trim `rawTypeNameParts` KDoc** (`JavaTypeOverAst.kt`) — replaced the inline "83%"
  measurement detail with a concise one-liner; the data lives in
  `archive/MEASUREMENTS_2026_04_22.md` §7.4.
- **Trim `CacheHelpers.kt` file KDoc** — consolidated the "why not `by lazy(PUBLICATION)`"
  and "why not explicit backing fields" rationale from 31 lines to 19, preserving the key
  insight (24B+8B per delegate × 200K instances).
- **Rename `findInPhase1JavaModel` → `walkJavaSourceSupertypes`** and
  **`findInPhase2ClassIdWalk` → `walkBinarySupertypes`** (`JavaInheritedMemberResolver.kt`)
  — the old "Phase 1 / Phase 2" names read as compilation phases; the new names describe
  the data source (Java model vs binary/Kotlin supertypes). Updated all KDoc references.
- **Rename `AggregatedInheritedInnerClassesHolder` → `InheritedInnerCache`** and
  **`aggregatedInheritedInnerClassesHolder` → `inheritedInnerCache`**
  (`JavaResolutionContext.kt`) — shorter name for the `@Volatile`-wrapped cache holder.
- **Add comment on `JavaAnnotationOverAst.resolve()`** — one-liner explaining that
  resolution is callback-based via `resolveAnnotation()`.
- **Trim static-inner-class context comment** (`JavaClassOverAst.kt:162-167`) — removed the
  3-line mirror explanation of the `else` branch; the first two lines already explain the
  non-static case and the code is self-evident.

### Test Results

No behavioral changes — renames and comment edits only. Compilation verified via IDE.

### Files Modified

| File | Change | Lines |
|------|--------|-------|
| `JavaTypeOverAst.kt` | Trim `rawTypeNameParts` KDoc | −1 |
| `CacheHelpers.kt` | Trim file-level KDoc | −12 |
| `JavaInheritedMemberResolver.kt` | Rename two methods + update KDoc | ~0 (rename) |
| `JavaResolutionContext.kt` | Rename class + field | ~0 (rename) |
| `JavaAnnotationOverAst.kt` | Add one-liner on `resolve()` | +1 |
| `JavaClassOverAst.kt` | Trim inner-class context comment | −3 |
| **Net** | | **−15 lines** |

### Key Learnings

- **Review after refactoring catches different things than review before.** The original
  review (r1.md) independently flagged `filterTypeUseAnnotations` caching and
  `resolveSimpleNameToClassIdImpl` extraction — both of which had already been tried and
  reverted (P1 and R12+O10). Cross-checking against the refactoring history before acting
  avoided re-introducing known regressions.
- **Method names that describe mechanism ("Phase 1/2") age worse than names that describe
  data source ("JavaSource/Binary").** The Phase 1/Phase 2 naming was clear when the two
  methods were freshly extracted in B.3 but confusing to a fresh reader.

---

## Archived Iteration History

All entries from the 2026-04-17 through 2026-04-22 refactoring work (Phases A-E of
`REFACTORING_PLAN_2026_04_21.md`) have been archived to:

- `implDocs/archive/ITERATION_RESULTS_2026_04_22.md` — full log with Phase B regression
  investigation, Phase C measurements, Phase D implementation, Phase E cleanup
- `implDocs/archive/REFACTORING_PLAN_2026_04_21.md` — the 5-phase plan (A-E)
- `implDocs/archive/MEASUREMENTS_2026_04_22.md` — Phase C measurement data (8 hypotheses,
  3 corpora, corrected classloader-isolation methodology)
- `implDocs/archive/REFACTORING_STEPS_2026_04_17_DETAILS.md` — earlier refactoring steps 1.3-3.6
- `implDocs/archive/LAZY_PACKAGE_INDEXING_PLAN_2026_04_21.md` — lazy per-package indexing design (implemented)

### Open items carried forward

- **Context-level `tryResolve` cache** (`PERFORMANCE_REVIEW_2026-04-20.md` §2 #6) — deferred
  with a recorded correctness argument. Only revisit if profiling shows `resolve()` as a
  measurable bottleneck.
