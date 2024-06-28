// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A<E>
class B<E, F>

fun <K, V> A<K>.toB(f: (V) -> K, g: (K) -> V): B<K, V> = B()

operator fun <T1, E1> B<T1, E1>.getValue(o: Any, desc: KProperty<*>): E1 = TODO()
operator fun <T2, E2> B<T2, E2>.setValue(o: Any, desc: KProperty<*>, value: E2) {}

val q = A<String>()

class Test {
    var prop by q.toB({ "abc" }, { "cde" })
}