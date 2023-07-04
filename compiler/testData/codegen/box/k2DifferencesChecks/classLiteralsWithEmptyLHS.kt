// ORIGINAL: /compiler/testData/diagnostics/tests/callableReference/unsupported/classLiteralsWithEmptyLHS.fir.kt
// WITH_STDLIB
fun regular() {
    ::class

    with(Any()) {
        ::class
    }
}

fun Any.extension() {
    ::class
}

class A {
    fun member() {
        ::class
    }
}

fun box() = "OK"
