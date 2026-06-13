// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments
fun foo(x: Int) = ""

context(c: String)
fun foo(x: Long) = true

fun String.test() {
    val x1: String = foo(1)

    val x2: Boolean = foo(1, c = "")
    val x3: Boolean = foo(1L)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
