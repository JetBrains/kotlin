// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

enum class X {

    B {
        val value2 = "OK"
        override val value = { value2 }
    };

    abstract val value: () -> String
}

fun box(): String {
    return X.B.value()
}
