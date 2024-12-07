// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-56409

fun main() {
    val flag = true
    consumeTicker(
        select(if (flag) null else <!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>num<!> -> num.dec() }<!>, null)
    )
}

fun <T> select(a: T, b: T): T = a

fun consumeTicker(ticker: Ticker?) {}

fun interface Ticker {
    fun tick(num: Int)
}
