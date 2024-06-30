package foo

enum class X {
    B {
        override val value2 = "K"
        override val value = "O" + this.value2.get(this.ordinal).toString()
    };

    abstract val value2: String
    abstract val value: String
}

fun box() = X.B.value