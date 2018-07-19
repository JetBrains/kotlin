// IGNORE_BACKEND: JVM_IR
enum class X {
    B {
        val value2 = "K"

        val value3: String
        init {
            fun foo() = value2
            value3 = "O" + foo()
        }

        override val value = value3
    };

    abstract val value: String
}

fun box() = X.B.value