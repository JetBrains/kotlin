// RUN_PIPELINE_TILL: BACKEND
fun foo(a : Any) {}

fun test() {
  foo(object {});
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration */
