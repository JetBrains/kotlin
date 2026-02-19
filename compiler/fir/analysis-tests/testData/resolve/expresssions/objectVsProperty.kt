// RUN_PIPELINE_TILL: FRONTEND
object <!REDECLARATION!>A<!>

val <!REDECLARATION!>A<!> = 10


fun foo() = A

fun bar() {
    val A = ""
    val b = A
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, objectDeclaration, propertyDeclaration,
stringLiteral */
