// LANGUAGE_VERSION: 1.2
// IGNORE_BACKEND: JS

enum class X {
    B {
        override val value = "OK"

        override val test = ::value.get()
    };

    abstract val value: String

    abstract val test: String
}

fun box() = X.B.test