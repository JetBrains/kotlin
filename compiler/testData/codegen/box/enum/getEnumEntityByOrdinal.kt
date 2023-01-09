// KT-55828
// IGNORE_BACKEND_K2: NATIVE
package foo

enum class X {
    B {
        override val value2 = "K"
        override val value = "O" + B.value2.get(X.B.ordinal).toString()
    };

    abstract val value2: String
    abstract val value: String
}

fun box() = X.B.value