// RUN_PIPELINE_TILL: BACKEND

fun foo(): String = ""

// Verifies that val _ is available even if the checker is disabled
fun bar() {
    foo()
    val _ = foo()
    val _ = foo()
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, propertyDeclaration, stringLiteral, unnamedLocalVariable */
