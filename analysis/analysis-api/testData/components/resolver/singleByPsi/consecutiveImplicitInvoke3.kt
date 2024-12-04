operator fun Int.invoke() : Long = 1L
operator fun Long.invoke() : Double = 1.0
operator fun Double.invoke() {}
fun test(i: Int) {
    <expr>i()()()</expr>
}