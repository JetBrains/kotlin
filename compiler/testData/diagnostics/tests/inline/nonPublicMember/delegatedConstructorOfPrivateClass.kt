// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76316
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline
private abstract class C
<!NOTHING_TO_INLINE!>inline<!> fun f(): Any = object : <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>C<!>() {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inline */
