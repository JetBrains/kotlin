// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81816

val nullableInt: Int? get() = null
fun error(msg: String): Nothing = null!!
fun unreachable() {}

fun test1(): Int {
    return nullableInt?.let { return it } ?: 0
    <!UNREACHABLE_CODE!>unreachable()<!>
}

fun test2(): Int {
    val it = nullableInt
    return if (it != null) {
        return it
    } else {
        0
    }
    <!UNREACHABLE_CODE!>unreachable()<!>
}

fun test3(a: Int?): Any? {
    a?.let {
        return a
    } <!UNREACHABLE_CODE!>?:<!> return null
    <!UNREACHABLE_CODE!>unreachable()<!>
}

fun test4(a: Int?) {
    a?.let {
        return
    } <!UNREACHABLE_CODE!>?:<!> error("null a")
    <!UNREACHABLE_CODE!>unreachable()<!>
}

/* GENERATED_FIR_TAGS: elvisExpression, equalityExpression, functionDeclaration, getter, ifExpression, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall, smartcast */
