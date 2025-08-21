// ISSUE: KT-78245
// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// MODULE: lib
// FILE: A.kt
enum class MyEnum {
    X, Y
}

private fun foo(x: MyEnum = X, y: MyEnum = x): MyEnum = y

internal inline fun bar(): MyEnum = foo()

// MODULE: main()(lib)
// FILE: B.kt
fun box(): String = when(bar()) {
    X -> "OK"
    else -> "FAIL"
}
