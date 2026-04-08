// NO_CHECK_LAMBDA_INLINING
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// MODULE: lib
// FILE: A.kt
enum class MyEnum {
    X, Y
}

private fun foo(x: MyEnum): MyEnum = x

internal inline fun bar(): MyEnum = foo(X)

// MODULE: main()(lib)
// FILE: B.kt
fun box(): String = when(bar()) {
    X -> "OK"
    else -> "FAIL"
}
