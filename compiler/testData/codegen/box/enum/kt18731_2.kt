
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
