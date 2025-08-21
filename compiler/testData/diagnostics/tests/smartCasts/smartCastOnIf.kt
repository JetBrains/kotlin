// RUN_PIPELINE_TILL: BACKEND
fun baz(s: String?): Int {
    return if (s == null) {
        ""
    }
    else {
        val u: String? = null
        if (u == null) return 0
        <!DEBUG_INFO_SMARTCAST!>u<!>
    }.length
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, localProperty,
nullableType, propertyDeclaration, smartcast, stringLiteral */
