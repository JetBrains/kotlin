// FIR_IDENTICAL
// WITH_STDLIB

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6


class MyClass(val value: String)

operator fun MyClass.provideDelegate(host: Any?, p: Any): String =
        this.value

operator fun String.getValue(receiver: Any?, p: Any): String =
        this

val testO by MyClass("O")
val testK by "K"
val testOK = testO + testK
