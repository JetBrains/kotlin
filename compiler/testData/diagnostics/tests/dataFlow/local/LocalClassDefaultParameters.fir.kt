// RUN_PIPELINE_TILL: BACKEND
fun test(x: Any) {
  if (x !is String) return

  class Local(s: String = x) {
    fun foo(s: String = x): String = s
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, localClass, primaryConstructor,
smartcast */
