// JVM_ABI_K1_K2_DIFF: KT-62775

enum class X {
    B {
        val value2 = "K"

        val anonObject = object {
            val value3 = "O" + value2

            override fun toString(): String = value3
        }

        override val value = anonObject.toString()
    };

    abstract val value: String
}

fun box() = X.B.value