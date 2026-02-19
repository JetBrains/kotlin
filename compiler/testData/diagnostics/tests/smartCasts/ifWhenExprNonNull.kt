// RUN_PIPELINE_TILL: BACKEND
fun baz(s: String?): String {
    val t = if (s == null) {
        ""
    }
    else {
        val u: String? = null
        when (u) {
            null -> ""
            else -> <!DEBUG_INFO_SMARTCAST!>u<!>
        }
    }
    return t
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
