inline fun calculateB(): Int {
    val calculator = object {
        fun calc(): Int = 42
    }
    return calculator.calc()
}
