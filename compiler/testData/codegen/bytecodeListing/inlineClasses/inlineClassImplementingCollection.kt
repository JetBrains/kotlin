inline class InlineIterator<T>(private val it: Iterator<T>) : Iterator<T> {
    override fun hasNext(): Boolean = it.hasNext()
    override fun next(): T = it.next()
}

inline class InlineMutableIterator<T>(private val it: MutableIterator<T>) : MutableIterator<T> {
    override fun hasNext(): Boolean = it.hasNext()
    override fun next(): T = it.next()
    override fun remove() { it.remove() }
}

inline class InlineIterable<T>(private val it: Iterable<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> = it.iterator()
}

inline class InlineMutableIterable<T>(private val it: MutableIterable<T>) : MutableIterable<T> {
    override fun iterator(): MutableIterator<T> = it.iterator()
}

inline class InlineCollection<T>(private val c: Collection<T>) : Collection<T> {
    override val size: Int get() = c.size
    override fun contains(element: T): Boolean = c.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = c.containsAll(elements)
    override fun isEmpty(): Boolean = c.isEmpty()
    override fun iterator(): Iterator<T> = c.iterator()
}

inline class InlineMutableCollection<T>(private val mc: MutableCollection<T>) : MutableCollection<T> {
    override val size: Int get() = mc.size
    override fun contains(element: T): Boolean = mc.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = mc.containsAll(elements)
    override fun isEmpty(): Boolean = mc.isEmpty()
    override fun add(element: T): Boolean = mc.add(element)
    override fun addAll(elements: Collection<T>): Boolean = mc.addAll(elements)
    override fun clear() { mc.clear() }
    override fun iterator(): MutableIterator<T> = mc.iterator()
    override fun remove(element: T): Boolean = mc.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = mc.removeAll(elements)
    override fun retainAll(elements: Collection<T>): Boolean = mc.retainAll(elements)
}

inline class InlineList<T>(private val list: List<T>) : List<T> {
    override val size: Int get() = list.size
    override fun contains(element: T): Boolean = list.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = list.containsAll(elements)
    override fun get(index: Int): T = list[index]
    override fun indexOf(element: T): Int = list.indexOf(element)
    override fun isEmpty(): Boolean = list.isEmpty()
    override fun iterator(): Iterator<T> = list.iterator()
    override fun lastIndexOf(element: T): Int = list.lastIndexOf(element)
    override fun listIterator(): ListIterator<T> = list.listIterator()
    override fun listIterator(index: Int): ListIterator<T> = list.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<T> = list.subList(fromIndex, toIndex)
}

inline class InlineMutableList<T>(private val mlist: MutableList<T>) : MutableList<T> {
    override val size: Int get() = mlist.size
    override fun contains(element: T): Boolean = mlist.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = mlist.containsAll(elements)
    override fun get(index: Int): T = mlist[index]
    override fun indexOf(element: T): Int = mlist.indexOf(element)
    override fun isEmpty(): Boolean = mlist.isEmpty()
    override fun iterator(): MutableIterator<T> = mlist.iterator()
    override fun lastIndexOf(element: T): Int = mlist.lastIndexOf(element)
    override fun add(element: T): Boolean = mlist.add(element)
    override fun add(index: Int, element: T) { mlist.add(index, element) }
    override fun addAll(index: Int, elements: Collection<T>): Boolean = mlist.addAll(index, elements)
    override fun addAll(elements: Collection<T>): Boolean = mlist.addAll(elements)
    override fun clear() { mlist.clear() }
    override fun listIterator(): MutableListIterator<T> = mlist.listIterator()
    override fun listIterator(index: Int): MutableListIterator<T> = mlist.listIterator(index)
    override fun remove(element: T): Boolean = mlist.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = mlist.removeAll(elements)
    override fun removeAt(index: Int): T = mlist.removeAt(index)
    override fun retainAll(elements: Collection<T>): Boolean = mlist.retainAll(elements)
    override fun set(index: Int, element: T): T = mlist.set(index, element)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = mlist.subList(fromIndex, toIndex)
}

inline class InlineSet<T>(private val s: Set<T>) : Set<T> {
    override val size: Int get() = s.size
    override fun contains(element: T): Boolean = s.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = s.containsAll(elements)
    override fun isEmpty(): Boolean = s.isEmpty()
    override fun iterator(): Iterator<T> = s.iterator()
}

inline class InlineMutableSet<T>(private val ms: MutableSet<T>) : MutableSet<T> {
    override val size: Int get() = ms.size
    override fun contains(element: T): Boolean = ms.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = ms.containsAll(elements)
    override fun isEmpty(): Boolean = ms.isEmpty()
    override fun add(element: T): Boolean = ms.add(element)
    override fun addAll(elements: Collection<T>): Boolean = ms.addAll(elements)
    override fun clear() { ms.clear() }
    override fun iterator(): MutableIterator<T> = ms.iterator()
    override fun remove(element: T): Boolean = ms.remove(element)
    override fun removeAll(elements: Collection<T>): Boolean = ms.removeAll(elements)
    override fun retainAll(elements: Collection<T>): Boolean = ms.retainAll(elements)
}

inline class InlineMap<K, V>(private val map: Map<K, V>) : Map<K, V> {
    override val entries: Set<Map.Entry<K, V>> get() = map.entries
    override val keys: Set<K> get() = map.keys
    override val size: Int get() = map.size
    override val values: Collection<V> get() = map.values
    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: V): Boolean = map.containsValue(value)
    override fun get(key: K): V? = map[key]
    override fun isEmpty(): Boolean = map.isEmpty()
}

inline class InlineMutableMap<K, V>(private val mmap: MutableMap<K, V>) : MutableMap<K, V> {
    override val size: Int get() = mmap.size
    override fun containsKey(key: K): Boolean = mmap.containsKey(key)
    override fun containsValue(value: V): Boolean = mmap.containsValue(value)
    override fun get(key: K): V? = mmap[key]
    override fun isEmpty(): Boolean = mmap.isEmpty()
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = mmap.entries
    override val keys: MutableSet<K> get() = mmap.keys
    override val values: MutableCollection<V> get() = mmap.values
    override fun clear() { mmap.clear() }
    override fun put(key: K, value: V): V? = mmap.put(key, value)
    override fun putAll(from: Map<out K, V>) { mmap.putAll(from) }
    override fun remove(key: K): V? = mmap.remove(key)
}

inline class InlineMapEntry<K, V>(private val e: Map.Entry<K, V>) : Map.Entry<K, V> {
    override val key: K get() = e.key
    override val value: V get() = e.value
}

inline class InlineMutableMapEntry<K, V>(private val e: MutableMap.MutableEntry<K, V>) : MutableMap.MutableEntry<K, V> {
    override val key: K get() = e.key
    override val value: V get() = e.value
    override fun setValue(newValue: V): V = e.setValue(newValue)
}