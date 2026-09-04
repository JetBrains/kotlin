// RUN_PIPELINE_TILL: BACKEND
interface T {
    fun getX() = 1
}

<!CONFLICTING_JVM_DECLARATIONS!><!>interface C : T {
    val x: Int
        <!ACCIDENTAL_OVERRIDE!>get()<!> = 1
}

/* GENERATED_FIR_TAGS: functionDeclaration, getter, integerLiteral, interfaceDeclaration, propertyDeclaration */
