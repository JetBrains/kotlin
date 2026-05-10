// LANGUAGE: +CollectionLiterals
// DUMP_CFG
// RUN_PIPELINE_TILL: BACKEND

import A.Companion.of

class A {
    companion object {
        operator fun of(vararg args: Any): A = A()
    }

    operator fun plusAssign(other: A) { }
}

fun test(a: A) {
    a += []
}

fun ref(a: A) {
    a += of()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator, vararg */
