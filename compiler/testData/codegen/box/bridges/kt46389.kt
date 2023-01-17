// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6

// The code in this test should be prohibited in the frontend, see KT-36188.

// This test fails with an IllegalStateException on the JS(-IR) backend,
// but the behavior would probably not match the JVM backend even if
// the test passed. Compare with kt46389_jvmDefault.

interface I {
    fun h(x: String = "O"): Any
}

interface I2 : I

open class A {
    inline fun h(x: String = "K") = x
}

class B : A(), I2

fun box(): String {
    return "${(B() as I).h()}${B().h()}"
}
