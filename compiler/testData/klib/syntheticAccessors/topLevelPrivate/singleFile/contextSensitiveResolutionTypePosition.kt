// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface MySealed {
    class Ok(): MySealed
    class Fail(): MySealed
}

private fun foo(x: MySealed = MySealed.Ok()): MySealed = x

internal inline fun bar(): MySealed = foo()

fun box(): String = when(bar()) {
    is Ok -> "OK"
    is Fail -> "FAIL"
}
