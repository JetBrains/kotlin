// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
error class E1
error class E2

typealias R = Int | E1
typealias R2 = R
typealias R3 = R | E2
typealias S = String
typealias SR = S | E1

typealias F = String | G
typealias G = E1 | E2

fun r2(): R2 = 1
fun r3(): R3 = E2()
fun sr(): SR = ""
fun f(): F = E2()
fun g(): G = E1()

fun test(a: R2, b: R3, c: F) {
    val a1: Int | E1 = a
    val b1: Int | E1 | E2 = b
    val c1: String | E1 | E2 = c
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
stringLiteral, typeAliasDeclaration */
