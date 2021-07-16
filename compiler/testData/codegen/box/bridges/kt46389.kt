// IGNORE_BACKEND: JS, JS_IR

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
