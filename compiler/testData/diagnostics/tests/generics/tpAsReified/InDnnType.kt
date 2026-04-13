// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74516

inline fun <reified R> inline(r: R, any: Any): R? {
    if (any is R) return any
    return null
}

fun <T> bad(x: T): T? {
    if (x != null) return <!TYPE_PARAMETER_AS_REIFIED!>inline<!>(x, "")
    return null
}

fun main() {
    bad(1)?.toFloat() // CCE if no error
}

/* GENERATED_FIR_TAGS: dnnType, equalityExpression, functionDeclaration, ifExpression, inline, integerLiteral,
isExpression, nullableType, reified, safeCall, smartcast, stringLiteral, typeParameter */
