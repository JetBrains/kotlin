interface I1
interface I2
open class C

interface A1<K, V> where V : K, V : <!REPEATED_BOUND!>K<!>
interface A2<K, V, <!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>W<!>> where W : K, W : V
interface A3<K, <!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>V<!>> where V : I1, V : K, V : I2
interface A4<<!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>K<!>, V> where K : I1, K : I2, K : C, K : V, V : I2, V : I1

fun <K, V> f1() where V : K, V : <!REPEATED_BOUND!>K<!> {}
fun <K, V, <!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>W<!>> f2() where W : K, W : V {}
