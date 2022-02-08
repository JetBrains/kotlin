// WITH_STDLIB
// WITH_SIGNATURES

import java.util.*

class SortedMapImpl<A : Comparable<A>, B>(private val map: SortedMap<A, B>) : SortedMap<A, B> {
    override fun containsKey(key: A): Boolean = map.containsKey(key)
    override fun containsValue(value: B): Boolean = map.containsValue(value)
    override fun get(key: A): B? = map.get(key)
    override fun isEmpty(): Boolean = map.isEmpty()
    override fun clear() = map.clear()
    override fun put(key: A, value: B): B? = map.put(key, value)
    override fun putAll(from: Map<out A, B>) = map.putAll(from)
    override fun remove(key: A): B? = map.remove(key)
    override fun comparator(): Comparator<in A> = map.comparator()
    override fun subMap(fromKey: A, toKey: A): SortedMap<A, B> = map.subMap(fromKey, toKey)
    override fun headMap(toKey: A): SortedMap<A, B> = map.headMap(toKey)
    override fun tailMap(fromKey: A): SortedMap<A, B> = map.tailMap(fromKey)
    override fun firstKey(): A = map.firstKey()
    override fun lastKey(): A = map.lastKey()
    override val entries: MutableSet<MutableMap.MutableEntry<A, B>> get() = map.entries
    override val keys: MutableSet<A> get() = map.keys
    override val values: MutableCollection<B> get() = map.values
    override val size: Int get() = map.size
}
