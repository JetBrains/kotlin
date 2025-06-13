// RUN_PIPELINE_TILL: BACKEND
fun test(a: Any?) {
    when (a) {
        is String -> {
            val s = a
            s.length
        }
        "" -> {
            val s = a
            s.hashCode()
        }
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, isExpression, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
