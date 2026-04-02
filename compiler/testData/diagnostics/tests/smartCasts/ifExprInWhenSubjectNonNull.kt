// RUN_PIPELINE_TILL: BACKEND
fun baz(s: String?, u: String?): String {
    val t = when(if (u == null) return "" else u) {
        "abc" -> u
        "" -> {
            if (s == null) return ""
            s
        }
        else -> u
    }
    return t
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
