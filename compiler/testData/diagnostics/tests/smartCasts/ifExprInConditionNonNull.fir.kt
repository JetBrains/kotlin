// RUN_PIPELINE_TILL: BACKEND
fun baz(s: String?, b: Boolean?): String {
    val t = if (if (b == null) return "" else b) {
        if (s == null) return ""
        s
    }
    else {
        if (s != null) return s
        ""
    }
    return t
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral */
