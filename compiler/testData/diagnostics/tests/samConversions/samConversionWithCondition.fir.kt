fun main() {
    val flag = true
    consumeTicker(
        if (flag) null else <!ARGUMENT_TYPE_MISMATCH!>{ num -> num.<!UNRESOLVED_REFERENCE!>dec<!>() }<!>
    )
}

fun consumeTicker(ticker: Ticker?) {

}

fun interface Ticker {
    fun tick(num: Int)
}
