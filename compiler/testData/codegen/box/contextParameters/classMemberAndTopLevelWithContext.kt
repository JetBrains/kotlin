// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

class A {
    fun foo(): String = "class fun"
    val b: String = "class val"
}

fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)

context(a: A)
fun foo(): String = "context fun"

context(a: A)
val b: String
    get() = "context val"

fun box(): String {
    return if (
        (A().foo() == "class fun") &&
        (A().b == "class val") &&
        (context(A()) {
            foo() == "context fun" && b == "context val"
        })
    ) "OK" else "NOK"
}
