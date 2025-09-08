// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class Ccc() {
    fun foo() = 1
}

interface Ttt {
    fun foo(): Int
}

class Zzz() : Ccc(), Ttt

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, primaryConstructor */
