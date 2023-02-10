// ISSUE: KT-38288

open class A<K : Any>

class G : A<G.Key<*>>() {
    class Key<T : Any> {}
}
