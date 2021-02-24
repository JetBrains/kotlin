package f

fun <T> f(i: Int, t: T, c: MutableCollection<T>) {}
fun <T> f(a: Any, t: T, l: MutableList<T>) {}

fun test(l: List<Int>) {
    <!NONE_APPLICABLE!>f<!>(1, "", l)
}
