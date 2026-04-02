// RUN_PIPELINE_TILL: BACKEND
fun baz(s: String?, u: String?): String {
    val t = when(s) {
        is String -> {
            if (u == null) return s
            u
        }
        else -> {
            if (u == null) return ""
            u
        }
    }
    return t
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, isExpression, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral, whenExpression, whenWithSubject */
