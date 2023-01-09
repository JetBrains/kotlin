// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class X {
    B {
        override val value = "OK"

        override val test = ::value.get()
    };

    abstract val value: String

    abstract val test: String
}

fun box() = X.B.test
