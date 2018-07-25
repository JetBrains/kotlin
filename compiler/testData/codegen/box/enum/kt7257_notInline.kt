// IGNORE_BACKEND: JVM_IR
fun <T, R> T.letNoInline(fn: (T) -> R) =
        fn(this)

enum class X {
    B {
        val value2 = "K"
        override val value = "O".letNoInline { it + value2 }
    };

    abstract val value: String
}

fun box() = X.B.value