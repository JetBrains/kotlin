// RUN_PIPELINE_TILL: BACKEND
fun foo() {
    class C {
        <!CONFLICTING_JVM_DECLARATIONS!>val x<!> = 1
        <!CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localClass, propertyDeclaration */
