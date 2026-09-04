// RUN_PIPELINE_TILL: BACKEND
interface B {
    fun getX() = 1
}

interface D {
    val x: Int
}

class <!ACCIDENTAL_OVERRIDE!>C(d: D)<!> : D by d, B

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, integerLiteral,
interfaceDeclaration, primaryConstructor, propertyDeclaration */
