// RUN_PIPELINE_TILL: BACKEND
interface T {
    val x: Int
        get() = 1
}

class C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, interfaceDeclaration,
propertyDeclaration */
