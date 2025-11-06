// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81915
fun foo() {}
fun main() {
    when (@UnresolvedAnnotation foo()) {
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, whenExpression, whenWithSubject */
