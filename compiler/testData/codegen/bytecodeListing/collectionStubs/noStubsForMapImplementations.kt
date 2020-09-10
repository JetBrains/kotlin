// Ensure the proper collection stubs are added, in
// particular *not* when specialized implementations are provided.

// IGNORE_BACKEND: JVM_IR
// Extra stub generated as override of e.g. `iterator` is not detected.
// Complicated by the fact the the overrides are properties codegen'ed
// as methods (e.g. entries -> entrySet)and the need to look at type
// parameters when determining overrides.
class MyMap<K, V> : Map<K, V> {

    class MySet<E> : Set<E> {
        override fun contains(element: E): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun iterator(): Iterator<E> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun isEmpty(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override fun containsAll(elements: Collection<E>): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
        override val size: Int
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    }

    override val entries
        get() = MySet<Map.Entry<K,V>>()
    override val keys
        get() = MySet<K>()
    override val size: Int
        get() = TODO("not implemented")
    override val values
        get() = ArrayList<V>()

    override fun containsKey(key: K) = TODO("not implemented")
    override fun containsValue(value: V) = TODO("not implemented")
    override fun get(key: K) = TODO("not implemented")
    override fun isEmpty() = TODO("not implemented")
}