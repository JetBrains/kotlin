// RUN_PIPELINE_TILL: BACKEND
class C {
    companion object {
        <!CONFLICTING_JVM_DECLARATIONS!>val x<!> = 1
        <!CONFLICTING_JVM_DECLARATIONS!>fun getX()<!> = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, objectDeclaration,
propertyDeclaration */
