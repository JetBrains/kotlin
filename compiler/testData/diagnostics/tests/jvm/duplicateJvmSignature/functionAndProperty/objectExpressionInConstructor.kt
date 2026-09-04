// RUN_PIPELINE_TILL: BACKEND
class Outer {
    val x = object {
        <!CONFLICTING_JVM_DECLARATIONS!>val x<!> = 1
        <!CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, integerLiteral,
propertyDeclaration */
