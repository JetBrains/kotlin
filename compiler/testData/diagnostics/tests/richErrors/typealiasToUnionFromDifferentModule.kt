// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// MODULE: m1
error class E1
error class E2

typealias Errs = E1 | E2
typealias R = Int | E1
typealias Res<T> = List<T> | E1

// MODULE: m2(m1)
fun errs(): Errs = E1()
fun r(): R = 1
fun rOrE2(): R | E2 = E2()
fun res(): Res<String> = listOf("")

fun test(a: R, b: Errs, c: Res<String>) {
    val a1: Int | E1 = a
    val b1: E1 | E2 = b
    val c1: List<String> | E1 = c
}

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
