// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
error class E1
error class E2
error class E3

typealias Errs = E1 | E2
typealias R = Int | E1
typealias NR = Int? | E1
typealias NE1 = Nothing? | E1
typealias NE1x = E1?

fun errs(): Errs = E1()
fun r(): R = 1
fun nr(): NR = null
fun rOrE2(): R | E2 = E2()
fun stringOrErrs(): String | Errs = E2()
fun nullableR(): R? = null
fun nullableErrs(): Errs? = null
fun nullableErrsOrE3(): Errs? | E3 = E3()
fun dedup(): E1 | Errs = E2()
fun nothingOrErrs(): Nothing | Errs = E1()

fun ne1OrN2(): NE1 | E2 = E1()
fun ne1xOrN2(): <!NULLABLE_ERROR_COMPONENT_IN_UNION_TYPE!>NE1x | E2<!> = E1()

fun assignability(a: R, b: Int | E1, c: R | E2, d: Int | E1 | E2) {
    val a1: Int | E1 = a
    val b1: R = b
    val c1: Int | E1 | E2 = c
    val d1: R | E2 = d
}

fun smartCasts(x: R, e: Errs, u: Int | E1 | E2) {
    if (x is Int) return
    if (x is E1) return
    when (e) {
        is E1 -> {}
        is E2 -> {}
    }
    if (u is <!CANNOT_CHECK_FOR_ERASED!>Errs<!>) {
        u
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, integerLiteral, intersectionType,
isExpression, localProperty, nullableType, propertyDeclaration, smartcast, typeAliasDeclaration, whenExpression,
whenWithSubject */
