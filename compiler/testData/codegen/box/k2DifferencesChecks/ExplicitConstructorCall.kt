// ORIGINAL: /compiler/testData/diagnostics/tests/enum/ExplicitConstructorCall.fir.kt
// WITH_STDLIB
// KT-7753: attempt to call enum constructor explicitly
enum class A(val c: Int) {
    ONE(1),
    TWO(2);
    
    fun createA(): A {
        // Error should be here!
        return A(10)
    }
}

fun box() = "OK"
