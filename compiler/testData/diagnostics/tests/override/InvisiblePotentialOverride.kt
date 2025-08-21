// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class A {
    private fun foo() : Int = 1
}

class B : A() {
    fun foo() : String = ""
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, stringLiteral */
