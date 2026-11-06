// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// FILE: a.kt
error class E1
error class E2

// `G` is declared in a later file, so it is still unresolved when this file's
// supertypes are applied. Each union component must be visited eagerly.
typealias F = String | G
typealias H = List<G> | E1

fun f(): F = E2()
fun h(): H = listOf(E1())

fun test(c: F, d: H) {
    val c1: String | E1 | E2 = c
    val d1: List<E1 | E2> | E1 = d
}

// FILE: b.kt
typealias G = E1 | E2

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, typeAliasDeclaration */
