// RUN_PIPELINE_TILL: CODEGEN
open class B {
    fun getX() = 1
}

class C(<!ACCIDENTAL_OVERRIDE!>val x: Int<!>) : B()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
