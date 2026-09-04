// RUN_PIPELINE_TILL: BACKEND
class C(<!CONFLICTING_JVM_DECLARATIONS!>val x: Int<!>) {
    <!CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, primaryConstructor, propertyDeclaration */
