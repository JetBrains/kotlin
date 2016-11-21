impl fun <T> f1(): T = throw AssertionError()

impl fun <T> f2(t: T) {}

impl fun <K : Enum<K>, V : MutableList<out K>> f3(v: Map<V, Enum<in K>>, w: Comparable<*>) {}

impl fun <T: Comparable<T>> Array<out T>.sort(): Unit {
    java.util.Arrays.sort(this)
}

impl class C1<A>
impl class C2<B : C2<B>>
impl class C3<D, E : MutableList<in D>>

impl abstract class AbstractList<F> : MutableList<F>, java.io.Serializable
