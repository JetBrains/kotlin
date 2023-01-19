fun main() {
    val flag = true
    consumeTicker(
        if (flag) null else { num -> num.dec() }
    )
}

fun consumeTicker(ticker: Ticker?) {

}

fun interface Ticker {
    fun tick(num: Int)
}
