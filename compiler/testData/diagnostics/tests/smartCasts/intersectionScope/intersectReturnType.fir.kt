// SKIP_TXT
class C<T>(val value: T)

fun <T> assignable(x: () -> T) {}

fun <T, V> foo(t: C<out T>, v: C<out V>) {
    assignable<T> { t.value } // sure
    assignable<V> { v.value } // obviously
    if (t == v) {
        // => {t,v} is C<out T> & C<out V>
        // => {t,v}.value is T & V
        assignable<T> { t.value }
        assignable<V> { v.value }
        assignable<T> { v.value }
        assignable<V> { t.value }
    }
}
