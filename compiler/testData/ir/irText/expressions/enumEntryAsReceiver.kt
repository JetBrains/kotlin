// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

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
