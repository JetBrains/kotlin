// RUN_PIPELINE_TILL: CODEGEN
class C {
    companion object {
        val X = 1
        <!CONFLICTING_JVM_DECLARATIONS!>val `X$1`<!> = 1
    }

    <!CONFLICTING_JVM_DECLARATIONS!>val X<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, integerLiteral, objectDeclaration, propertyDeclaration */
