// FIR_IDENTICAL

fun <T> select(vararg x: T) = x[0]
fun <T> id1(x: T): T = x
fun <T> id2(x: T): T = x

fun test() {
    fun foo() {}
    select(id1(::foo), id2 { })
}
