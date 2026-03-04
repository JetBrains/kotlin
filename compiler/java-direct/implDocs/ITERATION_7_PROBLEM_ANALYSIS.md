# Iteration 7: Problem Analysis

**Date**: 2026-03-04  
**Test Status**: 90/138 passing (65.2%), 48 failing

## Overview

This document captures the root cause analysis of the 48 remaining failing tests after Iteration 6. The analysis was performed using systematic categorization of error types and exception-based debugging to understand AST structures.

---

## Problem Categories Summary

| Category | Count | Severity | Fix Complexity |
|----------|-------|----------|----------------|
| MISSING_DEPENDENCY_CLASS | 15 | High | Complex |
| Wrong NPE behavior | 6 | Medium | Medium |
| NOTHING_TO_OVERRIDE | 5 | High | **Simple** |
| NONE_APPLICABLE | 4 | Medium | Medium |
| CANNOT_INFER_PARAMETER_TYPE | 3 | Medium | Medium |
| UNRESOLVED_REFERENCE | 3 | Medium | Varies |
| TYPE_MISMATCH | 2 | Medium | Medium |
| NPE expected but not thrown | 2 | Low | Medium |
| Other (single occurrences) | 8 | Low | Varies |

---

## Detailed Analysis

### 1. Array Types Not Parsed (5 tests) - NOTHING_TO_OVERRIDE

**Affected Tests:**
- testOverrideWithArrayParameterType
- testOverrideWithArrayParameterType2
- testOverrideWithArrayParameterTypeNotNull
- testOverrideWithVarargParameterType
- testPrimitiveSubstitutionToDnnParameter

**Symptom:**
```
NOTHING_TO_OVERRIDE: 'foo' overrides nothing. 
Potential signatures for overriding: fun foo(a: String!): String!
```

But Java interface declares: `String foo(String[] a)` - the array dimension is lost.

**Root Cause:**
The `createJavaType()` function in `JavaTypeOverAst.kt` doesn't handle array types. It only checks for:
1. Primitive types (via `_KEYWORD`)
2. Reference types (via `JAVA_CODE_REFERENCE`)

**AST Structure Discovery** (via exception-based debugging):
```
TYPE: String[]
  TYPE: String
    JAVA_CODE_REFERENCE: String
      IDENTIFIER: String
      REFERENCE_PARAMETER_LIST: 
  LBRACKET: [
  RBRACKET: ]
```

**Fix Required:**
In `createJavaType()`, detect `LBRACKET` child and recursively create `JavaArrayTypeOverAst` with nested `TYPE` as component type.

**Impact:** High - 5 tests directly, potentially more indirectly

---

### 2. Kotlin Classes from Java Code (15 tests) - MISSING_DEPENDENCY_CLASS

**Affected Tests:**
- testFunctionWithBigArity
- testGenericSamSmartcast
- testJavaToKotlinHierarchy
- testKjkWithRawTypes
- testKt42824, testKt42825
- testLambdaInstanceOf
- testLocalEntities
- testNoAssertionForNulllableCaptured
- testOverrideWithGenericArrayParameterType
- testPropertyVarianceConflict
- testSamUnboundTypeParameter
- testUsingJavaAtomicWhenKotlinAtomicExpected
- testUsingKotlinAtomicWhenJavaAtomicExpected
- testUsingNullableValueAsLowerBoundLeadsToNullableResult2

**Symptom:**
```
MISSING_DEPENDENCY_CLASS: Cannot access class 'Function'. 
Check your module classpath for missing or conflicting dependencies.
```

**Root Cause:**
Java source files import Kotlin classes like:
```java
import kotlin.Function;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.FunctionN;
```

The `CombinedJavaClassFinder` falls back to binary class finder for JDK/library classes, but Kotlin compiled classes (`.class` files from kotlin-stdlib) are not being found.

**Investigation Needed:**
- Check if binary finder is configured to include kotlin-stdlib in classpath
- Verify Kotlin class metadata is accessible via platform infrastructure

**Impact:** High - 15 tests, but fix may be complex/architectural

---

### 3. Type Parameter Resolution (7+ tests)

**Affected Tests (CANNOT_INFER_PARAMETER_TYPE):**
- testSamTypeParameter
- testVarargCall1
- testVarargCall2

**Affected Tests (NONE_APPLICABLE):**
- testAddedOverloadWithAtomics
- testIntersectionKotlinJavaAtomics
- testKotlinToJavaHierarchy
- testKt48590

**Symptom:**
```
CANNOT_INFER_PARAMETER_TYPE: Cannot infer type for type parameter 'R'. Specify it explicitly.
```

**Root Cause:**
Two issues identified:

1. **`JavaTypeParameterOverAst.upperBounds`** returns empty list:
   ```kotlin
   override val upperBounds: Collection<JavaClassifierType> get() = emptyList()
   ```
   Type bounds like `<T extends Comparable<T>>` are not parsed.

2. **`JavaMethodOverAst.typeParameters`** returns empty list:
   ```kotlin
   override val typeParameters: List<JavaTypeParameter> get() = emptyList()
   ```
   Method-level type parameters like `<R> R foo()` are not parsed.

**Fix Required:**
- Parse `EXTENDS_BOUND_LIST` in `JavaTypeParameterOverAst`
- Parse `TYPE_PARAMETER_LIST` in `JavaMethodOverAst`

---

### 4. Enhanced Nullability / NPE Assertions (8 tests)

**Wrong NPE behavior (6 tests):**
- testInFunctionWithExpressionBody
- testInLocalFunctionWithExpressionBody
- testInLocalVariableInitializer
- testInMemberPropertyInitializer
- testInPropertyGetterWithExpressionBody
- testInTopLevelPropertyInitializer

**NPE expected but not thrown (2 tests):**
- testNnStringVsTXArray
- testNnStringVsTXString

**Symptom:**
```
AssertionFailedError: expected: <OK> but was: <Fail: should throw>
```
or
```
AssertionError: NullPointerException expected
```

**Root Cause:**
Likely related to nullability annotation handling. The tests expect NPE to be thrown when null is passed to a non-null parameter, but the assertion is either:
- Not being generated (annotations not parsed)
- Being generated incorrectly

**Investigation Needed:**
- Check annotation parsing in `JavaClassOverAst.annotations`
- Verify `@NotNull`/`@Nullable` annotations are recognized

---

### 5. Interface Field Access (1 test) - UNRESOLVED_REFERENCE

**Affected Test:** testJavaInterfaceFieldDirectAccess

**Symptom:**
```
UNRESOLVED_REFERENCE: Unresolved reference
```

**Root Cause:**
Interface fields (constants) may not be properly exposed. Need to check `JavaClassOverAst.fields` implementation for interfaces.

---

### 6. Runtime Errors (2 tests)

**AbstractMethodError (1 test):** testDelegationToJavaDnn
```
AbstractMethodError: Receiver class C does not define or inherit an implementation of the resolved method 'abstract java.lang.Object foo()' of interface J.
```

**NoSuchMethodError (1 test):** testInheritanceWithWildcard
```
NoSuchMethodError: 'Y D.foo()'
```

**Root Cause:**
These are runtime errors suggesting method signature mismatches in generated bytecode. Likely caused by incorrect type resolution leading to wrong method signatures being generated.

---

## Debugging Techniques Used

### Exception-Based AST Inspection

Since println/logging doesn't appear in Gradle test output, used exception throwing to inspect AST structure:

```kotlin
// In JavaValueParameterOverAst.type getter:
if (typeNode.text.contains("[")) {
    throw IllegalStateException(
        "DEBUG ARRAY TYPE: node.text='${node.text}', " +
        "typeNode.text='${typeNode.text}', " +
        "typeNode.dump=\n${typeNode.dump()}"
    )
}
```

This revealed the AST structure for array types, enabling the fix design.

### Test Failure Categorization Script

Used Python script to parse JUnit XML results and categorize failures:

```python
import xml.etree.ElementTree as ET
import glob
from collections import defaultdict

results_dir = "compiler/java-direct/build/test-results/test"
failures = defaultdict(list)

for xml_file in glob.glob(f"{results_dir}/*.xml"):
    tree = ET.parse(xml_file)
    for testcase in tree.findall('.//testcase'):
        failure = testcase.find('failure')
        if failure is not None:
            # Categorize by error pattern
            text = (failure.text or '') + (failure.get('message', '') or '')
            if 'MISSING_DEPENDENCY_CLASS' in text:
                failures['MISSING_DEPENDENCY_CLASS'].append(name)
            # ... more patterns
```

---

## Recommended Fix Priority

### Phase 1: Quick Wins (5+ tests)
1. **Array type parsing** - Simple fix, 5 direct test fixes

### Phase 2: Type Parameters (7+ tests)
2. **Type parameter bounds** - Parse `EXTENDS_BOUND_LIST`
3. **Method type parameters** - Parse `TYPE_PARAMETER_LIST` in methods

### Phase 3: Complex Issues (15+ tests)
4. **Kotlin classes from Java** - Requires investigation of binary finder configuration
5. **Enhanced nullability** - Requires annotation parsing investigation

---

## Files to Modify

| File | Changes Needed |
|------|----------------|
| `JavaTypeOverAst.kt` | Add array type handling in `createJavaType()` |
| `JavaTypeOverAst.kt` | Parse `upperBounds` in `JavaTypeParameterOverAst` |
| `JavaMemberOverAst.kt` | Parse `typeParameters` in `JavaMethodOverAst` |
| `JavaDirectComponentRegistrar.kt` | Investigate Kotlin class resolution |

---

*Document created: 2026-03-04*
