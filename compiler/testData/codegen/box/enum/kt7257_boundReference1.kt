// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// LANGUAGE_VERSION: 1.2

enum class X {
    B {
        val k = "K"

        inner class Inner {
            fun foo() = "O" + k
        }

        val inner = Inner()

        val bmr = inner::foo

        override val value = bmr.invoke()
    };

    abstract val value: String
}

fun box() = X.B.value