// RUN_PIPELINE_TILL: BACKEND
fun test() {
  val out : Int? = null
  val x : Nothing? = null
  if (out != x)
    out.plus(1)
  if (out == x) return
  out.plus(1)
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, localProperty,
nullableType, propertyDeclaration, smartcast */
