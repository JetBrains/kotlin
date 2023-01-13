// KT-55828
// IGNORE_BACKEND_K2: NATIVE
// SKIP_DCE_DRIVEN

enum class Bar {
    ONE {
        override fun toString(): String {
            if (this != TWO && this == ONE) return "OK" else return "FAIL"
        }
    },
    TWO;
}

fun box(): String {
    return Bar.ONE.toString()
}
