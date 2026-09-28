// RUN_PIPELINE_TILL: CODEGEN
open class B {
    fun getX() = 1
}

class C : B() {
    <!ACCIDENTAL_OVERRIDE!>val x: Int<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
