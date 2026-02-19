// ISSUE: KT-78245
// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

private fun foo(x: MyEnum = X, y: MyEnum = x): MyEnum = y

internal inline fun bar(): MyEnum = foo()

fun box(): String = when(bar()) {
    X -> "OK"
    else -> "FAIL"
}
