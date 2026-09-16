// RUN_PIPELINE_TILL: CODEGEN
interface T {
    val x: Int
}

abstract class C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, propertyDeclaration */
