// RUN_PIPELINE_TILL: BACKEND
fun set(key : String, value : String) {
  val a : String? = ""
  when (a) {
    "" -> a.get(0)
    is String, is Any -> a.compareTo("")
    else -> a.toString()
  }
}

/* GENERATED_FIR_TAGS: disjunctionExpression, equalityExpression, functionDeclaration, integerLiteral, isExpression,
localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
