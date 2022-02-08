// ISSUE: KT-39046

fun foo(b: B<Int, Int>) {}

fun test_1(b: B<String, Number>) {
    foo(b.myMap {
        it.k.length // implicits
    } <!UNCHECKED_CAST!>as B<Int, Int><!>)
}

fun test_2(s: String) {
    val func = { s.length } <!UNCHECKED_CAST!>as B<Int, Int><!>
}

class B<out K, V>(val k: K, val v: V)

fun <X, R, V> B<X, V>.myMap(transform: (B<X, V>) -> R): B<R, V> = TODO()
