# Java-Direct: Iteration Results Log

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Last Updated**: 2026-03-23 (iter 53)

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

---

## Iteration 52: Multi-Dimensional Array Types - 2026-03-23

### Root Cause Analysis
The KMP Java parser places multi-dimensional array brackets as **siblings** under the same TYPE node:
```
TYPE: List<Double>[][]
  TYPE: List<Double>        ← inner base type
    JAVA_CODE_REFERENCE: List<Double>
  LBRACKET: [               ← first dimension (siblings, not nested)
  RBRACKET: ]
  LBRACKET: [               ← second dimension
  RBRACKET: ]
```

`createJavaType()` only found one `LBRACKET` and created one `JavaArrayTypeOverAst`, silently dropping additional dimensions. This turned `List<Double>[][]` into `List<Double>[]`, causing raw type erasure to produce wrong member types and incorrect diagnostics.

### Fix
**File**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`

Changed both array-handling paths in `createJavaType()` to count ALL `LBRACKET` children and wrap that many `JavaArrayTypeOverAst` dimensions around the inner type. Annotations are attached only to the outermost dimension.

### Test Results
- Box: 1167/1168, Phased: 1444/1456, Total failing: 13

### Tests Fixed
- **Box**: `testJavaMethodsSmokeTest`, `testJavaArrayType` (reflection metadata now correct with proper array dimensions)
- **Phased**: `testArrays` (raw type array field assignments), `testRawSupertypeOverride` (method override matching with raw supertype)

### Remaining Raw Type Failures
- `testInterdependentTypeParameters` — different root cause (Boo<N> rendered as Boo<*,*,*> instead of Boo<*>)
- `testNonTrivialErasure` — different root cause (recursive bound erasure not producing correct type)

### Key Learnings
- KMP parser flattens array dimensions as siblings, unlike javac which nests `JCArrayTypeTree`
- Always dump AST structure when array or nested type handling is suspect

---

## Iteration 53: Annotation Default Binary Expressions - 2026-03-23

### Root Cause Analysis
Java annotation members can have default values that are compile-time constant expressions, e.g.:
```java
int i2() default 21212121 + 32323232;
String str() default "fi" + "zz";
```

`evaluateConstantExpression()` in `JavaAnnotationOverAst.kt` only handled `PREFIX_EXPRESSION` (negation) and `LITERAL_EXPRESSION`, but not `BINARY_EXPRESSION`. Binary expressions like `21212121 + 32323232` evaluated to `null`, which FIR converted to an error expression. When comparing the Java annotation's defaults against the Kotlin `expect` declaration's defaults, the mismatch triggered `ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE`.

### Fix
**File**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaAnnotationOverAst.kt`

Added `BINARY_EXPRESSION` handling to `evaluateConstantExpression()`:
- Extracts left/right operands and operator (`PLUS`, `MINUS`, `ASTERISK`, `DIV`, `PERC`)
- Recursively evaluates operands (supports nested expressions)
- `evaluateBinaryExpression()` handles string concatenation (`PLUS` with any `String` operand) and integer arithmetic
- `numericBinaryOp()` preserves `Long` vs `Int` type based on operands

### Test Results
- Box: 1168/1168, Phased: 1445/1456, Total failing: 11

### Tests Fixed
- **Box**: `testAnnotationsViaActualTypeAliasFromBinary` (annotation default value matching)
- **Phased**: `testAnnotationsViaActualTypeAlias2` (same root cause — `ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE` no longer spuriously reported)

### Key Learnings
- PSI evaluates constant expressions via javac's `PsiExpression.evaluate()`; java-direct must implement its own evaluator
- Annotation default values flow through `annotationParameterDefaultValue` → FIR's `toFirExpression` → `createConstantOrError`, where `null` becomes an error expression

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
