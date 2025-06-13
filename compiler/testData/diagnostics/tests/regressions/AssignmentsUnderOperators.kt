// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun test() {
  var a : Any? = null
  if (a is Any) else a = null;
  while (a is Any) a = null
  while (true) a = null
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, ifExpression, isExpression, localProperty, nullableType,
propertyDeclaration, whileLoop */
