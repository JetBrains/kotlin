// RUN_PIPELINE_TILL: CODEGEN
interface T {
    val x: Int
        get() = 1
}

<!CONFLICTING_JVM_DECLARATIONS!><!>interface C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}

/* GENERATED_FIR_TAGS: functionDeclaration, getter, integerLiteral, interfaceDeclaration, propertyDeclaration */
