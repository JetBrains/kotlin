// IGNORE_BACKEND_FIR: JVM_IR
enum class X {
    B {
        val value2 = "K"
        override val value = "O".let { it + value2 }
    };

    abstract val value: String
}

fun box() = X.B.value