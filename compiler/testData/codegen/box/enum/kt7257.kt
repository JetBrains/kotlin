// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class X {
    B {
        val value2 = "K"
        override val value = "O".let { it + value2 }
    };

    abstract val value: String
}

fun box() = X.B.value