enum class X {
    B {
        val value2 = "K"

        val anonObject = object {
            override fun toString(): String =
                    "O" + value2
        }

        override val value = anonObject.toString()
    };

    abstract val value: String
}

fun box() = X.B.value