// RUN_PIPELINE_TILL: CODEGEN
<!CONFLICTING_JVM_DECLARATIONS!><!>interface T {
    val x: Int
        <!CONFLICTING_JVM_DECLARATIONS!>get()<!> = 1
    <!CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
}

/* GENERATED_FIR_TAGS: functionDeclaration, getter, integerLiteral, interfaceDeclaration, propertyDeclaration */
