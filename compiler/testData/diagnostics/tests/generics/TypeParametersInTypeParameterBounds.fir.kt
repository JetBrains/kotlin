interface I1
interface I2
open class C

interface A1<K, V> where V : K, V : K
interface A2<K, V, W> where W : K, W : V
interface A3<K, V> where V : I1, V : K, V : I2
interface A4<K, V> where K : I1, K : I2, K : C, K : V, V : I2, V : I1

fun <K, V> f1() where V : K, V : K {}
fun <K, V, W> f2() where W : K, W : V {
    fun <T> f3() where T : K, T : V {}
    fun <T> f4() where T : K, T : K {}
}
fun <K, V, W> f3() where W : K, W : V, W : Any {}
