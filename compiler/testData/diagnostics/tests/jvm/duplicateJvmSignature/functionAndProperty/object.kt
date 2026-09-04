// RUN_PIPELINE_TILL: BACKEND
object C {
    <!CONFLICTING_JVM_DECLARATIONS!>val x<!> = 1
    <!CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, objectDeclaration, propertyDeclaration */
