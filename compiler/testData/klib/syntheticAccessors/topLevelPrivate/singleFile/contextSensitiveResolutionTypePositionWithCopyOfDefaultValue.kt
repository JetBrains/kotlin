// ISSUE: KT-78245
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface MySealed {
    class Ok(): MySealed
    class Fail(): MySealed
}

private fun foo(x: MySealed = MySealed.Ok(), y: MySealed = x): MySealed = y

internal inline fun bar(): MySealed = foo()

fun box(): String = when(bar()) {
    is Ok -> "OK"
    is Fail -> "FAIL"
}
