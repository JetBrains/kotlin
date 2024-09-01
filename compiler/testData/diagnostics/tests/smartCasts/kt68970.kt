// LANGUAGE: +ImprovedVarianceInCst
class A<V>

class B<K, V>

fun <K, V> foo(): B<K, A<V>> = B<K, A<V>>()

fun <K, V> takeB(p: B<K, A<V>>) {}

fun <K, V> test(w: B<K, A<V>>, b: Boolean) {
    val a = if (b) w else foo()
    takeB(<!DEBUG_INFO_SMARTCAST!>a<!>)

    val a2 = when {
        b -> w
        else -> foo()
    }
    takeB(<!DEBUG_INFO_SMARTCAST!>a2<!>)

    val a3 = select(w, foo())
    takeB(<!TYPE_MISMATCH!>a3<!>)
}

fun <T> select(vararg x: T): T = x[0]