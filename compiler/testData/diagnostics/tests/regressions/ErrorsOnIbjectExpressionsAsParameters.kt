// RUN_PIPELINE_TILL: CODEGEN
fun foo(a : Any) {}

fun test() {
  foo(object {});
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration */
