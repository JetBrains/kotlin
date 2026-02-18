// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
class A {
    inline fun <reified T> foo(x: T) = x
}

// FILE: main.kt
fun test(block: (A, String) -> String) = block(A(), "OK")

fun box() : String {
    return test(A::foo)
}
