// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt
error class E1
error class E2

// `TA` and `TB` are declared in a later file, so they have to be resolved on demand during the SUPER_TYPES phase
// to classify them as rich error or non-error components.
typealias A = TA | E2
typealias B = <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>E1 | TB<!>

fun a(): A = E1()
fun a2(): A = E2()
fun nested(): Int | A = 1
fun nested2(): Int | A = E1()

fun test(a: A, n: Int | A) {
    val a1: E1 | E2 = a
    val n1: Int | E1 | E2 = n
}

// FILE: b.kt
typealias TA = E1
typealias TB = String

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
typeAliasDeclaration */
