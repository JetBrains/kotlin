package jet

public trait Iterable<out T> {
    public fun iterator() : Iterator<T>
}

public trait MutableIterable<out T> : Iterable<T> {
    override fun iterator() : MutableIterator<T>
}

public trait Collection<out E> : Iterable<E>, Hashable {
    // Query Operations
    public fun size() : Int
    public fun isEmpty() : Boolean
    public fun contains(o : Any?) : Boolean
    override fun iterator() : Iterator<E>

    // Bulk Operations
    public fun containsAll(c : Collection<Any?>) : Boolean
}

public trait MutableCollection<E> : Collection<E>, MutableIterable<E> {
    // Query Operations
    override fun iterator() : MutableIterator<E>

    // Modification Operations
    public fun add(e : E) : Boolean
    public fun remove(o : Any?) : Boolean

    // Bulk Modification Operations
    public fun addAll(c : Collection<E>) : Boolean
    public fun removeAll(c : Collection<Any?>) : Boolean
    public fun retainAll(c : Collection<Any?>) : Boolean
    public fun clear()
}

public trait List<out E> : Collection<E> {
    // Query Operations
    override fun size() : Int
    override fun isEmpty() : Boolean
    override fun contains(o : Any?) : Boolean
    override fun iterator() : Iterator<E>

    // Bulk Operations
    override fun containsAll(c : Collection<Any?>) : Boolean

    // Positional Access Operations
    public fun get(index : Int) : E

    // Search Operations
    public fun indexOf(o : Any?) : Int
    public fun lastIndexOf(o : Any?) : Int

    // List Iterators
    public fun listIterator() : ListIterator<E>
    public fun listIterator(index : Int) : ListIterator<E>

    // View
    public fun subList(fromIndex : Int, toIndex : Int) : List<E>
}

public trait MutableList<E> : List<E>, MutableCollection<E> {
    // Modification Operations
    override fun add(e: E) : Boolean
    override fun remove(o : Any?) : Boolean

    // Bulk Modification Operations
    override fun addAll(c : Collection<E>) : Boolean
    public fun addAll(index : Int, c : Collection<E>) : Boolean
    override fun removeAll(c : Collection<Any?>) : Boolean
    override fun retainAll(c : Collection<Any?>) : Boolean
    override fun clear()

    // Positional Access Operations
    public fun set(index : Int, element : E) : E
    public fun add(index : Int, element : E)
    public fun remove(index : Int) : E

    // List Iterators
    override fun listIterator() : MutableListIterator<E>
    override fun listIterator(index : Int) : MutableListIterator<E>

    // View
    override fun subList(fromIndex : Int, toIndex : Int) : MutableList<E>
}

public trait Set<out E> : Collection<E> {
    // Query Operations
    override fun size() : Int
    override fun isEmpty() : Boolean
    override fun contains(o : Any?) : Boolean
    override fun iterator() : Iterator<E>

    // Bulk Operations
    override fun containsAll(c : Collection<Any?>) : Boolean
}

public trait MutableSet<E> : Set<E>, MutableCollection<E> {
    // Query Operations
    override fun iterator() : MutableIterator<E>

    // Modification Operations
    override fun add(e: E) : Boolean
    override fun remove(o : Any?) : Boolean

    // Bulk Modification Operations
    override fun addAll(c : Collection<E>) : Boolean
    override fun removeAll(c : Collection<Any?>) : Boolean
    override fun retainAll(c : Collection<Any?>) : Boolean
    override fun clear()
}

public trait Map<out K, out V> {
    // Query Operations
    public fun size() : Int
    public fun isEmpty() : Boolean
    public fun containsKey(key : Any?) : Boolean
    public fun containsValue(value : Any?) : Boolean
    public fun get(key : Any?) : V?

    // Views
    public fun keySet() : Set<K>
    public fun values() : Collection<V>
    public fun entrySet() : Set<Map.Entry<K, V>>

    public trait Entry<out K, out V> : Hashable {
        public fun getKey() : K
        public fun getValue() : V
    }
}

public trait MutableMap<K, V> : Map<K, V> {
    // Modification Operations
    public fun put(key : K, value : V) : V?
    public fun remove(key : Any?) : V?

    // Bulk Modification Operations
    public fun putAll(m : Map<out K, V>)
    public fun clear()

    // Views
    override fun keySet() : MutableSet<K>
    override fun values() : MutableCollection<V>
    override fun entrySet() : MutableSet<MutableMap.MutableEntry<K, V>>

    public trait MutableEntry<K,V> : Map.Entry<K, V>, Hashable {
    	public fun setValue(value : V) : V
    }
}