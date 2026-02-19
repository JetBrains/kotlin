// RUN_PIPELINE_TILL: BACKEND
// See KT-5737
fun get(): String? {
    return "abc"
}

fun foo(): Int {
    var ss:String? = get()

    return if (ss != null && ss.length > 0)
        1
    else
        0
}

/* GENERATED_FIR_TAGS: andExpression, comparisonExpression, equalityExpression, functionDeclaration, ifExpression,
integerLiteral, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral */
