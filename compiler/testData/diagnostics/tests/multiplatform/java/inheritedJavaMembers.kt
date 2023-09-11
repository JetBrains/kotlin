// FIR_IDENTICAL
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

interface MutableListEx<E> : MutableList<E> {
    fun removeRange(fromIndex: Int, toIndex: Int)
    fun addAll(elements: FastArrayList<E>): Boolean = addAll(elements as Collection<E>)
    fun setAddAll(index: Int, elements: FastArrayList<E>, offset: Int = 0, size: Int = elements.size - offset) {}
    fun setAll(index: Int, elements: FastArrayList<E>, offset: Int = 0, size: Int = elements.size - offset) {}
    fun addAll(elements: FastArrayList<E>, offset: Int = 0, size: Int = elements.size - offset) {}
    fun removeToSize(size: Int) {}
}

expect class FastArrayList<E> : MutableListEx<E>, RandomAccess {
    constructor()
    constructor(initialCapacity: Int)
    constructor(elements: Collection<E>)

    fun trimToSize()
    override fun removeRange(fromIndex: Int, toIndex: Int)
    fun ensureCapacity(minCapacity: Int)
    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: E): Boolean
    override fun containsAll(elements: Collection<E>): Boolean
    override operator fun get(index: Int): E
    override fun indexOf(element: E): Int
    override fun lastIndexOf(element: E): Int
    override fun iterator(): MutableIterator<E>
    override fun add(element: E): Boolean
    override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    override fun addAll(index: Int, elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear()
    override operator fun set(index: Int, element: E): E
    override fun add(index: Int, element: E)
    override fun removeAt(index: Int): E
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

public actual open class FastArrayList<E> internal constructor(
    var array: Array<Any?>,
    var _size: Int = array.size,
    var arrayCapacity: Int = array.size,
) : AbstractMutableList<E>(), MutableListEx<E>, RandomAccess {
    public actual constructor() : this(arrayOfNulls(16), 0) {}
    public actual constructor(initialCapacity: Int) : this(arrayOfNulls(initialCapacity), 0) {}
    public actual constructor(elements: Collection<E>) : this(elements.toTypedArray<Any?>()) {}

    public actual fun trimToSize() {}
    public actual fun ensureCapacity(minCapacity: Int) {}
    actual override val size: Int get() = _size
    @Suppress("UNCHECKED_CAST") actual override fun get(index: Int): E = array[0] as E
    actual override fun set(index: Int, element: E): E = element
    actual override fun add(element: E): Boolean = true
    actual override fun add(index: Int, element: E) {}
    actual override fun addAll(elements: Collection<E>): Boolean = true
    actual override fun addAll(index: Int, elements: Collection<E>): Boolean = true
    actual override fun remove(element: E): Boolean = true
    @Suppress("UNCHECKED_CAST") actual override fun removeAt(index: Int): E = array[0] as E
    actual override fun removeRange(fromIndex: Int, toIndex: Int) {}
    override fun setAll(index: Int, elements: FastArrayList<E>, offset: Int, size: Int) {}
    override fun addAll(elements: FastArrayList<E>, offset: Int, size: Int) {}
    actual override fun clear() {}
    actual override fun contains(element: E): Boolean = false
    actual override fun indexOf(element: E): Int = -1
    actual override fun lastIndexOf(element: E): Int = -1
}
