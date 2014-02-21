package kotlin

public trait Iterator<out T> {
    public fun next(): T
    public fun hasNext(): Boolean
}

public trait MutableIterator<out T> : Iterator<T> {
    public fun remove(): Unit
}

public trait ListIterator<out T> : Iterator<T> {
    // Query Operations
    override fun next(): T
    override fun hasNext(): Boolean

    public fun hasPrevious(): Boolean
    public fun previous(): T
    public fun nextIndex(): Int
    public fun previousIndex(): Int
}

public trait MutableListIterator<T> : ListIterator<T>, MutableIterator<T> {
    // Query Operations
    override fun next(): T
    override fun hasNext(): Boolean

    // Modification Operations
    override fun remove(): Unit
    public fun set(e: T): Unit
    public fun add(e: T): Unit
}
