// RUN_PIPELINE_TILL: FRONTEND
class Aaa() {
    val <!REDECLARATION!>a<!> = 1
    val <!REDECLARATION!>a<!> = ""
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, stringLiteral */
