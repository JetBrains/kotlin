// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

private fun foo(x: MyEnum): MyEnum = x

internal inline fun bar(): MyEnum = foo(X)

fun box(): String = when(bar()) {
    X -> "OK"
    else -> "FAIL"
}
