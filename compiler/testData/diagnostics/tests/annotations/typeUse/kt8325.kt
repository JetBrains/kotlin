// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// ISSUE: KT-8325

fun foo() {
    object : @<!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!> Any() {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration */
