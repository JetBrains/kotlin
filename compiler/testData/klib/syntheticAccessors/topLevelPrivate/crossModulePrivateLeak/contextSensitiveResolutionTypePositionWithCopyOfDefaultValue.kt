// ISSUE: KT-78245
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// MODULE: lib
// FILE: A.kt
sealed interface MySealed {
    class Ok(): MySealed
    class Fail(): MySealed
}

private fun foo(x: MySealed = MySealed.Ok(), y: MySealed = x): MySealed = y

internal inline fun bar(): MySealed = foo()

// MODULE: main()(lib)
// FILE: B.kt
fun box(): String = when(bar()) {
    is Ok -> "OK"
    is Fail -> "FAIL"
}
