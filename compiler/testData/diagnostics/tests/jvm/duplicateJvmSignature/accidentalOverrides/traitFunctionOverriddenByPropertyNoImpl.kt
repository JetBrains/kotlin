// RUN_PIPELINE_TILL: BACKEND
interface T {
    fun getX(): Int
}

abstract class C : T {
    val x: Int
        <!ACCIDENTAL_OVERRIDE!>get()<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, interfaceDeclaration,
propertyDeclaration */
