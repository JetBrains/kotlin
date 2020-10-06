// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.

// IGNORE_BACKEND: JVM_IR
// TODO KT-42067 JVM_IR generates extra bridges:
//      public bridge final method entrySet(): MyMap$MySet
//      public bridge final method keySet(): MyMap$MySet
//      public bridge final method values(): java.util.ArrayList
class MyMap<K, V> : Map<K, V> {

    class MySet<E> : Set<E> {
        override fun contains(element: E): Boolean = TODO()
        override fun iterator(): Iterator<E> = TODO()
        override fun isEmpty(): Boolean = TODO()
        override fun containsAll(elements: Collection<E>): Boolean = TODO()
        override val size: Int get() = TODO()
    }

    override val entries get() = MySet<Map.Entry<K,V>>()
    override val keys get() = MySet<K>()
    override val size: Int get() = TODO()
    override val values get() = ArrayList<V>()

    override fun containsKey(key: K): Boolean = TODO()
    override fun containsValue(value: V): Boolean = TODO()
    override fun get(key: K): V = TODO()
    override fun isEmpty(): Boolean = TODO()
}