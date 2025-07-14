// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: A.kt
sealed interface MySealed {
    class Ok(): MySealed
    class Fail(): MySealed
}

private fun foo(x: MySealed = MySealed.Ok()): MySealed = x

internal inline fun bar(): MySealed = foo()

// FILE: B.kt
fun box(): String = when(bar()) {
    is Ok -> "OK"
    is Fail -> "FAIL"
}
