actual fun <T> f1(): T = throw AssertionError()

actual fun <T> f2(t: T) {}

actual fun <K : Enum<K>, V : MutableList<out K>> f3(v: Map<V, Enum<in K>>, w: Comparable<*>) {}

actual fun <T: Comparable<T>> Array<out T>.sort(): Unit {
    java.util.Arrays.sort(this)
}

actual class C1<A>
actual class C2<B : C2<B>>
actual class C3<D, E : MutableList<in D>>

actual abstract class AbstractList<F> : MutableList<F>, java.io.Serializable
