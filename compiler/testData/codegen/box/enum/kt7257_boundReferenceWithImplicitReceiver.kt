// IGNORE_BACKEND_FIR: JVM_IR

enum class X {
    B {
        override val value = "OK"

        override val test = ::value.get()
    };

    abstract val value: String

    abstract val test: String
}

fun box() = X.B.test
