# Java-Direct Failure Analysis: Post-Iteration 17 Update

**Date**: 2026-03-06  
**Tests Run**: Box (1166), Phased (327)  
**Results After Iteration 17**: Box 90 failing, Phased 82 failing = 172 total failures (88.5% pass rate)

---

## Executive Summary

**Iteration 17 Results**: Fixed 4 tests (176 → 172 failures), NOT the expected ~30.

### Why Fewer Tests Fixed Than Expected

The original analysis **incorrectly conflated three different annotation-related issues**:

1. **Annotation Argument Values** (FIXED by iteration 17) — `@Foo(value = "x")` argument parsing
2. **Annotation Method Access** (NOT FIXED) — Accessing annotation interface methods as properties (`b.value` where `b` is annotation instance)
3. **Const Val Reference Resolution** (NOT FIXED) — `@Foo(CONST_VAL)` where the argument is a reference to a Kotlin const val

### Revised Category Summary

| Category | Count | Priority | Notes |
|----------|-------|----------|-------|
| Baseline/Content Diffs | 104 | VARIES | Need individual review |
| Nested Class Resolution | ~12 | HIGH | `Outer.Inner` in binary classes |
| Visibility Issues | 6 | MEDIUM | Protected/package-private access |
| Nullability TYPE_USE | 5 | MEDIUM | `List<@NotNull T>` |
| Missing Dep Superclass | 8 | HIGH | Supertype resolution |
| Const Val in Annotation Args | 2 | MEDIUM | Reference expression resolution |
| Annotation Method Access | 2 | LOW | Annotation instantiation feature |
| Other Edge Cases | ~30 | VARIES | Various |

---

## What Iteration 17 Actually Fixed

Iteration 17 implemented annotation argument subinterfaces correctly:
- `JavaLiteralAnnotationArgument` for literals ✅
- `JavaArrayAnnotationArgument` for arrays ✅  
- `JavaEnumValueAnnotationArgument` for enums ✅
- `JavaClassObjectAnnotationArgument` for `.class` ✅
- `JavaAnnotationAsAnnotationArgument` for nested annotations ✅

This fixed tests where annotation arguments were simple literals or enums.

---

## Remaining Issues (Corrected Analysis)

### Issue 1: Const Val Reference in Annotation Arguments (2 tests)

**Tests**: `testConstValAsAnnotationArgumentInJava`, `testFakeJvmNameInJava`

**Problem**: When annotation argument is a reference to a Kotlin const val:
```java
import static example.KotlinDtoMapping.ID;  // ID is a Kotlin const val

@SimpleAnnotation(ID)  // REFERENCE_EXPRESSION, not literal
public String getId() { ... }
```

The current code treats ALL `REFERENCE_EXPRESSION` as enum values, but const val references need to be resolved to their literal value or handled specially.

**Fix**: In `createAnnotationArgumentFromValue`, distinguish between:
- Enum constant references (`EnumClass.ENTRY`)
- Const val references (`KotlinDtoMapping.ID`)
- Static field references

### Issue 2: Annotation Method Access (2 tests)

**Tests**: `testJavaAnnotation`, `testClassArrayInAnnotation`

**Problem**: These tests use annotation instantiation (`B("OK")`) and access annotation interface methods as properties (`b.value`). This is a Kotlin language feature (`InstantiationOfAnnotationClasses`) that requires annotation interfaces to expose their methods.

**Root Cause**: NOT about annotation argument parsing. The annotation INTERFACE methods (`String value()`) need to be exposed as callable members.

**Fix Location**: `JavaClassOverAst` or `JavaMemberOverAst` — annotation interface methods need special handling.

### Issue 3: Baseline/Content Diffs (104 tests)

These are the majority of failures. The test output differs from expected baselines. Categories:
- May be legitimate differences in how java-direct represents types
- May indicate missing features
- May need baseline updates if behavior is correct

**Action**: Need individual triage to determine if each is a bug or acceptable difference.

---

## Original Category 1: Annotation Arguments Not Implemented

**STATUS: MOSTLY FIXED by Iteration 17**

Only 2 tests remain with `ANNOTATION_NULL_ARG_IR` error, and those are due to const val reference resolution, not basic annotation argument parsing.

### Symptoms
- `IR annotation has null argument` errors
- `UNRESOLVED_REFERENCE: Unresolved reference 'value'` for annotation properties

### Root Cause
`JavaAnnotationArgumentOverAst` only implements the base `JavaAnnotationArgument` interface with just `name`. It does NOT implement the required subinterfaces:
- `JavaLiteralAnnotationArgument` (for literal values like strings, ints)
- `JavaArrayAnnotationArgument` (for array values)
- `JavaEnumValueAnnotationArgument` (for enum constants)
- `JavaClassObjectAnnotationArgument` (for `Foo.class` references)
- `JavaAnnotationAsAnnotationArgument` (for nested annotations)

### Affected Tests
- `testAnnotationsOnJavaMembers` - IR annotation has null argument
- `testConstValAsAnnotationArgumentInJava` - IR annotation has null argument
- `testFakeJvmNameInJava` - IR annotation has null argument
- `testJavaAnnotation` - UNRESOLVED_REFERENCE: value
- `testClassArrayInAnnotation` - UNRESOLVED_REFERENCE: value
- Plus ~25 more annotation-related failures

### Reference Implementation
See `compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/java/structure/impl/annotationArgumentsImpl.kt`:
- Uses `JavaPsiFacade.constantEvaluationHelper.computeConstantExpression()` for literal evaluation
- Handles all argument types with proper dispatching

### Fix Approach
1. Parse the annotation argument VALUE expression from AST (not just the name)
2. Implement constant evaluation for literals (strings, numbers, booleans)
3. Implement `JavaLiteralAnnotationArgumentOverAst` for literal values
4. Implement `JavaArrayAnnotationArgumentOverAst` for array initializers `{a, b, c}`
5. Implement `JavaEnumValueAnnotationArgumentOverAst` for enum references
6. Implement `JavaClassObjectAnnotationArgumentOverAst` for `.class` references
7. Implement `JavaAnnotationAsAnnotationArgumentOverAst` for nested annotations
8. Update `JavaAnnotationArgumentOverAst` factory to dispatch to appropriate implementation

### Test Files for Verification
- `compiler/testData/codegen/box/annotations/instances/javaAnnotation.kt`
- `compiler/testData/codegen/box/javaInterop/constValAsAnnotationArgumentInJava.kt`

---

## Category 2: Nested Class Resolution (HIGH PRIORITY)

### Symptoms
- `MISSING_DEPENDENCY_CLASS: Cannot access class 'Outer.Inner'`
- `MISSING_DEPENDENCY_SUPERCLASS: Cannot access 'Map.Entry'`
- `MISSING_DEPENDENCY_CLASS: Cannot access class 'KotlinTypeChecker.TypeConstructorEquality'`

### Root Cause
When resolving nested class references like `Outer.Inner`, the java-direct module doesn't properly resolve dot-separated names that reference nested classes in external (binary) classes.

### Affected Tests
- `testSerializableBoundInnerConstructorRef` - `Outer.Inner` not resolved
- `testSerializableInnerConstructorRef` - `Outer.Inner` not resolved
- `testMapEntry` - `Map.Entry` not resolved (JDK class)
- `testSamWithEquals` - `KotlinTypeChecker.TypeConstructorEquality` not resolved
- `testHugeMixedCapturedType` - Class 'A' resolution issue
- `testOuterInnerClasses` - `KotlinInner` superclass issue

### Fix Approach
1. In `classifierQualifiedName` resolution, handle dot-separated names as potential nested class references
2. When `Foo.Bar` fails resolution, try resolving `Foo` first, then look for nested class `Bar`
3. Ensure the hybrid class finder properly supports nested class lookup in binary classes

### Test Files for Verification
- `compiler/testData/codegen/box/invokedynamic/serializable/serializableInnerConstructorRef.kt`
- `compiler/testData/codegen/box/builtinStubMethods/extendJavaClasses/mapEntry.kt`

---

## Category 3: TYPE_USE Annotations on Type Arguments (MEDIUM PRIORITY)

### Symptoms
- Tests expecting nullability checks fail with "should throw on get()"
- Annotation position not correctly recognized on generic type arguments

### Root Cause
TYPE_USE annotations like `@NotNull` on type arguments (`List<@NotNull Integer>`) are not being parsed/attached correctly.

### Affected Tests
- `testJavaCollectionOfNotNullToTypedArrayFailFast`
- `testJavaIteratorOfNotNullFailFast`
- `testJavaCollectionOfExplicitNotNullFailFast`

### Test File Pattern
```java
public static List<@NotNull Integer> listOfNotNull() { ... }
```

### Fix Approach
1. When parsing generic type arguments, check for annotations attached to each type argument
2. Ensure TYPE_USE annotations flow through to FIR type conversion
3. May require changes to `JavaTypeOverAst.typeArguments` parsing

---

## Category 4: Wildcard/Projection Edge Cases (MEDIUM PRIORITY)

### Symptoms
- `NoSuchMethodError: 'Y D.foo()'` - method signature mismatch
- `There should left no projections after capture conversion` - FIR crash
- `Expected <? extends Set> but got <Set>` - wildcard not preserved

### Root Cause
Complex wildcard scenarios involving:
- Covariant return type overrides with wildcards
- Nested generic types with wildcards (`JInner<Box<O1>, ?>`)
- Delegation with wildcard preservation

### Affected Tests
- `testInheritanceWithWildcard` - NoSuchMethodError due to fake override issue
- `testJavaGenericSynthProperty` - Star projection crash
- `testDelegatedMembers` - Wildcard not preserved in reflection

### Test File Pattern
```java
interface B extends A {
    @Override
    Y<? extends B> foo();  // Covariant override with wildcard
}
```

### Fix Approach
1. Review wildcard type construction in `JavaTypeOverAst`
2. Ensure proper `ConeKotlinTypeProjection` creation for wildcards
3. May need FIR-level fixes for fake override generation with wildcards

---

## Category 5: Raw Type Visibility/Field Access (LOW PRIORITY)

### Symptoms
- `INVISIBLE_REFERENCE: Cannot access 'field': it is protected`

### Root Cause
Protected field access through raw type inheritance not handled correctly.

### Affected Tests
- `testContractAndRawField`
- `testJavaAnnotationConstructorTypes`
- `testWeirdCharBuffers`

### Test File Pattern
```java
public abstract class Base<T> {
    protected T instance;  // Protected field in generic class
}
public class Derived extends Base<String> { }  // Raw-ish inheritance
```

### Fix Approach
1. Review visibility calculation for inherited fields
2. Ensure raw type erasure doesn't affect visibility

---

## Category 6: Baseline/Content Diffs (VARIES)

### Nature
52 BASELINE_DIFF + 5 CONTENT_DIFF failures indicate the compiler output differs from expected baselines. These could be:
- Legitimate differences in how java-direct parses types
- Missing features causing different diagnostics
- Annotation/modifier differences

### Approach
These need individual investigation. Many may resolve automatically when categories 1-2 are fixed.

---

## Recommended Fix Order

### Iteration 17: Annotation Arguments
**Impact**: ~30 tests  
**Effort**: Medium  
**Files to modify**: `JavaAnnotationOverAst.kt`

1. Implement `JavaLiteralAnnotationArgumentOverAst` with constant evaluation
2. Implement `JavaArrayAnnotationArgumentOverAst`
3. Implement `JavaEnumValueAnnotationArgumentOverAst`
4. Implement `JavaClassObjectAnnotationArgumentOverAst`
5. Add factory method to dispatch based on AST node type

### Iteration 18: Nested Class Resolution
**Impact**: ~10 tests  
**Effort**: Medium  
**Files to modify**: `JavaResolutionContext.kt`, `JavaTypeOverAst.kt`

1. Handle dot-separated class names as potential nested classes
2. Update resolution callback to try outer class + nested lookup

### Iteration 19: TYPE_USE on Type Arguments
**Impact**: ~5 tests  
**Effort**: High  
**Files to modify**: `JavaTypeOverAst.kt`

1. Parse annotations on type argument nodes
2. Attach to type arguments during construction

### Iteration 20: Wildcard Edge Cases
**Impact**: ~5 tests  
**Effort**: High  
**Files to modify**: `JavaTypeOverAst.kt`, possibly FIR

1. Debug specific failing cases
2. May require FIR-level changes

---

## Debugging Commands

```bash
# Run specific failing test
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Annotations\$Instances.testJavaAnnotation" -q

# Run all annotation tests
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Annotations.*" -q

# Update test baselines if needed
./gradlew :compiler:java-direct:test --tests "TestName" -Pkotlin.test.update.test.data=true
```

---

## Appendix: Full Test Failure List

### Box Test Failures (49 analyzed)
| Category | Tests |
|----------|-------|
| BASELINE_DIFF | 21 |
| ANNOTATION_NULL_ARG | 3 |
| ANNOTATION_VALUE_UNRESOLVED | 2 |
| MISSING_DEP_CLASS (nested) | 4 |
| MISSING_DEP_SUPER | 1 |
| NULLABILITY_CHECK_FAIL | 3 |
| ABSTRACT_MEMBER_NOT_IMPL | 3 |
| INVISIBLE_REFERENCE | 3 |
| NO_SUCH_METHOD | 1 |
| STAR_PROJECTION_BUG | 1 |
| WILDCARD_REFLECTION | 1 |
| OTHER | 6 |

### Phased Test Failures (40 analyzed)
| Category | Tests |
|----------|-------|
| DATA_MISMATCH | 27 |
| CONTENT_DIFF | 5 |
| MISSING_DEP_CLASS | 2 |
| MISSING_DEP_SUPER | 2 |
| ANNOTATION_VALUE_UNRESOLVED | 1 |
| TYPE_MISMATCH | 1 |
| OTHER | 2 |

---

*Generated: 2026-03-06*
