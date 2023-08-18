// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// !JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8

// The code in this test should be prohibited in the frontend, see KT-36188.

// Before the fix to kt46389, the IR backend would have added a bridge
// for `h$default` in `B`, which would have lead both calls to use "K"
// as the default value. This would arguably be less surprising, but is
// impossible to match without jvm-default on the JVM(-IR)backend.
//
// Meanwhile, the two calls using "K" as the default value is the current
// behavior on the JS backend, which is why the test is muted for the JS
// backend.

interface I {
    fun h(x: String = "O"): Any
}

interface I2 : I

open class A {
    fun h(x: String = "K") = x
}

class B : A(), I2

fun box(): String {
    return "${(B() as I).h()}${B().h()}"
}
