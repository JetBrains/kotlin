public interface MyMap<K, V> {
    fun get(k: K): V
}

typealias MyMapAlias<X, Y> = MyMap<X, Y>

abstract class A<T, E>(val m: MyMapAlias<T, E>) : MyMapAlias<T, E> by m

class B : A<String, String>(object : MyMap<String, String> {
    override fun get(w: String): String {
        return w + "K"
    }
})

fun box(): String {
    return B().get("O")
}
