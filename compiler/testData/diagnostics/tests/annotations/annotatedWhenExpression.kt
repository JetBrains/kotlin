// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81915
fun foo() {}
fun main() {
    when (@<!DEBUG_INFO_MISSING_UNRESOLVED!>UnresolvedAnnotation<!> foo()) {
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, whenExpression, whenWithSubject */
