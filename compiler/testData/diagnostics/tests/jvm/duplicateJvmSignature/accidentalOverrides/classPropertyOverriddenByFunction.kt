// RUN_PIPELINE_TILL: BACKEND
open class B {
    val x: Int
        get() = 1
}

class C : B() {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, propertyDeclaration */
