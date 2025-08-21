// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79622
object Foo {
    private val x = { 0 }
}

fun main() {
    Foo.<!INVISIBLE_REFERENCE!>x<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, objectDeclaration, propertyDeclaration */
