class Foo

fun <T> type(): T = null!!

fun test() {
    var f = type<Foo>()
    <expr>++f</expr>
}

operator fun <R> R.inc(): R = null!!
