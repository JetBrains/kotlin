# Iteration 7: Problem Analysis

**Date**: 2026-03-04  
**Initial Status**: 90/138 passing (65.2%), 48 failing  
**Current Status**: 96/138 passing (69.6%), 42 failing (after Iteration 7a + 7b fixes)

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

### 1. ✅ FIXED: Array Types Not Parsed (5 tests) - NOTHING_TO_OVERRIDE

**Status**: Fixed in Iteration 7a

**Affected Tests:**
- ✅ testOverrideWithArrayParameterType - FIXED
- ❌ testOverrideWithArrayParameterType2 - Still fails (raw type `List...`)
- ✅ testOverrideWithArrayParameterTypeNotNull - FIXED
- ✅ testOverrideWithVarargParameterType - FIXED
- testPrimitiveSubstitutionToDnnParameter - Status TBD

**Fix Applied:**
In `createJavaType()`, detect `LBRACKET`/`ELLIPSIS` child and recursively create `JavaArrayTypeOverAst` with nested `TYPE` as component type.

---

### 2. ⚠️ PARTIALLY FIXED: Kotlin Classes from Java Code - MISSING_DEPENDENCY_CLASS

**Status**: Import resolution fixed in Iteration 7b, but some tests still fail with different errors

**Original Symptom:**
```
MISSING_DEPENDENCY_CLASS: Cannot access class 'FunctionN'. 
```

**Root Cause Found:**
The KMP Java parser emits `ERROR_ELEMENT` instead of `IMPORT_STATEMENT` for imports starting with reserved words like `kotlin`:
```java
import kotlin.jvm.functions.FunctionN;  // Parsed as ERROR_ELEMENT!
```

**Fix Applied (Iteration 7b):**
Modified `extractImports()` in `JavaResolutionContext.kt` to also process `ERROR_ELEMENT` nodes containing `IMPORT_KEYWORD`, reconstructing FQN from IDENTIFIER children.

**Current Status:**
- `testFunctionWithBigArity` - Now fails with `ARGUMENT_TYPE_MISMATCH` instead of `MISSING_DEPENDENCY_CLASS` (proves import resolution works!)
- Some tests still show `MISSING_DEPENDENCY_CLASS: Cannot access class 'T'` or `'U'` - these are **type parameters being treated as class names**, which is a DIFFERENT issue

**Remaining MISSING_DEPENDENCY_CLASS issues** are likely caused by:
1. Type parameters (`T`, `U`) being interpreted as class names
2. Issues with generic type argument resolution

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
