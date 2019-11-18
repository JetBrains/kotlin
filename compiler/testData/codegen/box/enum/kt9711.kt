// IGNORE_BACKEND_FIR: JVM_IR
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