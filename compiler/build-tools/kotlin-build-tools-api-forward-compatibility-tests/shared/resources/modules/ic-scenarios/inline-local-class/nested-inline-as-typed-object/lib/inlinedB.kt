interface NiceType {
    fun calc(): Int
}

inline fun calculateB(): Int {
    val calculator = object : NiceType {
        override fun calc(): Int = 42
    }
    return calculator.calc()
}
