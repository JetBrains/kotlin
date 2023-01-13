// KT-55828
// IGNORE_BACKEND_K2: NATIVE
enum class X {
    B {

        override val value = "OK"

        val bmr = B::value.get()

        override fun foo(): String {
            return bmr
        }
    };

    abstract val value: String

    abstract fun foo(): String
}

fun box() = X.B.foo()