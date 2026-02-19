// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80330

object Foo {
    private val <!REDECLARATION!>x<!> = { 0 }
    private val <!REDECLARATION!>x<!> = ""
}

fun main() {
    Foo.<!FUNCTION_EXPECTED, NONE_APPLICABLE!>x<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, objectDeclaration, propertyDeclaration,
stringLiteral */
