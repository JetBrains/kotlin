import java.util.*
import java.util.function.BiConsumer

private val DEBUG = true

abstract class SomeHashTable<K : Any, V : Any> : AbstractMutableMap<K, V>() {
    override fun forEach(action: BiConsumer<in K, in V>) {}

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            if (DEBUG) {
                return Collections.unmodifiableSet(
                    mutableSetOf<MutableMap.MutableEntry<K, V>>().apply {
                        forEach { key, value -> add(Entry(key, value)) }
                    }
                )
            }
            throw UnsupportedOperationException()
        }

    private class Entry<K, V>(override val key: K, override val value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V = throw UnsupportedOperationException()
    }
}