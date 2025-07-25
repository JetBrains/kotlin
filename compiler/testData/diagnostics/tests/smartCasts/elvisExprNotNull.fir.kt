// RUN_PIPELINE_TILL: FRONTEND
fun foo(s: Any?): String {
    val t = when {
        // To resolve: String U Nothing? = String?
        s is String -> s
        else -> null
    } ?: ""
    return t
}

fun bar(s: Any?): String {
    // To resolve: String U Nothing? = String?
    val t = (if (s == null) {
        null
    }
    else {
        val u: Any? = null
        if (u !is String) return ""
        u
    }) ?: "xyz"
    // Ideally we should have smart cast to String here
    return t
}

fun baz(s: String?, r: String?): String {
    val t = r ?: when {
        s != null -> s
        else -> ""
    }
    return t
}

fun withNull(s: String?): String {
    val t = s <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>
    // Error: nullable
    return <!RETURN_TYPE_MISMATCH!>t<!>
}

/* GENERATED_FIR_TAGS: elvisExpression, equalityExpression, functionDeclaration, ifExpression, isExpression,
localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, whenExpression */
