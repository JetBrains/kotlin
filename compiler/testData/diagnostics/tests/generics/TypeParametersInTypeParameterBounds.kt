interface I1
interface I2
open class C

interface A1<K, V> where V : K, V : <!REPEATED_BOUND!>K<!>
interface A2<K, V, W> where W : K, W : <!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>V<!>
interface A3<K, <!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>V<!>> where V : I1, V : K, V : I2
interface A4<<!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>K<!>, V> where K : I1, K : I2, K : C, K : V, V : I2, V : I1

fun <K, V> f1() where V : K, V : <!REPEATED_BOUND!>K<!> {}
fun <K, V, W> f2() where W : K, W : <!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>V<!> {
    fun <T> f3() where T : K, T : <!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>V<!> {}
    fun <T> f4() where T : K, T : <!REPEATED_BOUND!>K<!> {}
}
fun <K, V, <!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>W<!>> f3() where W : K, W : V, W : Any {}
