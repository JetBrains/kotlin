// RUN_PIPELINE_TILL: BACKEND
fun test(a: Any?) {
    when (a) {
        is String -> {
            val s = a
            <!DEBUG_INFO_SMARTCAST!>s<!>.length
        }
        "" -> {
            val s = a
            <!DEBUG_INFO_SMARTCAST!>s<!>.hashCode()
        }
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, isExpression, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
