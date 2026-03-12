# Test Failure Analysis After Test Suite Restoration

**Date**: 2026-03-12  
**Context**: Test suite restored from ~1166 to ~2500 tests  
**Current Results**: 
- Box: 1167 total, 33 failed (97.2% pass)
- Phased: 1442 total, 158 failed (89.0% pass)

## Box Test Failures (33 tests)

### Category 1: REGRESSION - Constant Evaluation (4 tests)
**Related to**: Iteration 23 (cross-language constants)  
**Error**: `CONST_VAL_WITH_NON_CONST_INITIALIZER`

| Test | Issue |
|------|-------|
| testAccessComplexConst | Complex const from Java not evaluated |
| testDifferentTypes | Const types from Java not evaluated |
| testKt29833 | Const evaluation regression |
| testJavaConstAnnotationArguments | `ANNOTATION_ARGUMENT_MUST_BE_CONST` |

**Analysis**: The iteration 23 `resolveInitializerValue` callback may not be handling all cases. These tests reference Java constants that use Kotlin constants.

**Action**: Investigate `ConstantEvaluator.kt` and the FIR callback in `lazyInitializer`.

---

### Category 2: REGRESSION - Visibility/Protected Access (7 tests)
**Error**: `INVISIBLE_REFERENCE`

| Test | Issue |
|------|-------|
| testProtectedStatic | Protected static member access |
| testProtectedStatic2 | Protected static member access |
| testStaticImportFromEnumJava | Static import from enum |
| testJavaVisibility | Java visibility modifiers |
| testWeirdCharBuffers | CharBuffer protected members |
| testContractAndRawField | Contract with raw field |
| testJavaAnnotationConstructorTypes | Annotation constructor types |

**Analysis**: Protected members in Java classes may not be correctly marked as visible. This could be a regression in visibility calculation or a new issue exposed by expanded tests.

**Action**: Check `JavaMemberOverAst.visibility` and how protected is handled for static members.

---

### Category 3: REGRESSION - Supertype Resolution (4 tests)
**Error**: `MISSING_DEPENDENCY_SUPERCLASS` / `MISSING_DEPENDENCY_CLASS`

| Test | Issue |
|------|-------|
| testIrrelevantImplCharSequence | CharSequence supertype |
| testIrrelevantImplCharSequenceWithExtraSupertype | Extra supertype |
| testIrrelevantImplMutableListSubstitution | MutableList substitution |
| testCapturedSelfInsideIntersection4 | Intersection type |

**Analysis**: Supertype resolution in iteration 21 may not handle all cases. These involve complex generic supertypes.

**Action**: Check `JavaClassOverAst.supertypes` and supertype resolution callbacks.

---

### Category 4: NEW - Enum Entries (1 test)
**Error**: `NoSuchFieldError: entries`

| Test | Issue |
|------|-------|
| testEnumEntriesFromJava | Java enum missing `entries` property |

**Analysis**: The `entries` synthetic property isn't being generated for Java enums. Related to iteration 17c `enumEntriesOrigin` work but may need additional handling.

**Action**: Check `FirJavaFacade.kt` enum entries generation for java-direct enums.

---

### Category 5: NEW - Type Inference (2 tests)
**Error**: `CANNOT_INFER_PARAMETER_TYPE`

| Test | Issue |
|------|-------|
| testApproximationForDefinitelyNotNull | DefinitelyNotNull approximation |
| testGenericBoundInnerConstructorRef | Generic bound inner constructor |

**Analysis**: Type inference with complex Java generics. Likely new tests not previously in suite.

**Action**: Lower priority - investigate after regressions fixed.

---

### Category 6: NEW - Nested Class (1 test)
**Error**: `UNRESOLVED_REFERENCE`

| Test | Issue |
|------|-------|
| testInheritedInnerAndNested | Inherited inner/nested classes |

**Analysis**: Nested class resolution from iteration 18 may not handle inherited inner classes.

**Action**: Check `JavaResolutionContext.resolveNestedClass()` for inherited cases.

---

### Category 7: NEW - Abstract Member (1 test)
**Error**: `ABSTRACT_MEMBER_NOT_IMPLEMENTED`

| Test | Issue |
|------|-------|
| testJavaMapWithCustomEntries | Map with custom entries |

**Analysis**: Custom Map.Entry implementation not recognized as implementing abstract member.

**Action**: Lower priority - investigate method signature matching.

---

### Category 8: BASELINE DIFF ONLY (7 tests)
These may just need baseline file updates:

| Test | Notes |
|------|-------|
| testAbstractMutableList_modCount | Multiplatform K2 |
| testActualizeExpectProtectedToJavaProtected | Multiplatform K2 |
| testAnnotationsViaActualTypeAliasFromBinary | Binary annotations |
| testCallableReferenceToJavaField | Callable reference |
| testCannotAccessInterfaceMemberViaReceiver | Interface member |
| testConstValAsAnnotationArgumentInJava | IR issue |
| testNotErasedMapGetMap_inherited | Map inheritance |

**Action**: Run with `-Pkotlin.test.update.test.data=true` to see if baselines can be updated, or if real errors exist.

---

## Priority Recommendations

### High Priority (Regressions from iterations 17-23)
1. **Constant Evaluation (4 tests)** - Core functionality, iteration 23 regression
2. **Visibility/Protected (7 tests)** - May affect many tests
3. **Enum Entries (1 test)** - Related to iteration 17c work

### Medium Priority (Supertype/Nested)
4. **Supertype Resolution (4 tests)** - Iteration 21 related
5. **Nested Class (1 test)** - Iteration 18 related

### Low Priority (New issues)
6. **Type Inference (2 tests)** - Complex new tests
7. **Abstract Member (1 test)** - Edge case
8. **Baseline Diffs (7 tests)** - May not be real issues

---

## Phased Test Analysis (158 failures)

The phased tests have 158 failures (vs 33 box failures). Many are likely the same root causes:
- BASELINE_DIFF patterns
- Same visibility/constant issues

**Recommendation**: Fix box test regressions first, then re-run phased tests to see how many are resolved.

---

## Summary

| Category | Count | Type |
|----------|-------|------|
| Constant Eval Regression | 4 | REGRESSION |
| Visibility Regression | 7 | REGRESSION |
| Supertype Regression | 4 | REGRESSION |
| Enum Entries | 1 | NEW (related to 17c) |
| Type Inference | 2 | NEW |
| Nested Class | 1 | NEW (related to 18) |
| Abstract Member | 1 | NEW |
| Baseline Diff | 7 | UNKNOWN |
| **Uncategorized** | 6 | UNKNOWN |
| **Total** | 33 | |

**Key Finding**: ~15 tests (45%) appear to be regressions from iterations 17-23 work, particularly:
- Constant evaluation (iteration 23)
- Visibility handling
- Enum entries (iteration 17c)

**Recommended Next Step**: Start with constant evaluation failures as they're clearly related to recent iteration 23 work.
