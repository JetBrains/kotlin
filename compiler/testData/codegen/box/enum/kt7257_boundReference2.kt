enum class X {
    B {

        override val value = "OK"

        val bmr = this::value.get()

        override fun foo(): String {
            return bmr
        }
    };

    abstract val value: String

    abstract fun foo(): String
}

fun box() = X.B.foo()