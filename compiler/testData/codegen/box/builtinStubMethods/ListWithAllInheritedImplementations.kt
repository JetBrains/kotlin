// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

open class Super<T>(val v: T) {
    public fun add(e: T): Boolean = true
    public fun remove(o: T): Boolean = true
    public fun addAll(c: Collection<T>): Boolean = true
    public fun addAll(index: Int, c: Collection<T>): Boolean = true
    public fun removeAll(c: Collection<T>): Boolean = true
    public fun retainAll(c: Collection<T>): Boolean = true
    public fun clear() {}
    public fun set(index: Int, element: T): T = element
    public fun add(index: Int, element: T) {}
    public fun removeAt(index: Int): T = v
}

class MyList<T>(v: T): Super<T>(v), List<T> {
    override val size: Int get() = 0
    override fun isEmpty(): Boolean = true
    override fun contains(o: T): Boolean = false
    override fun iterator(): Iterator<T> = throw Error()
    override fun containsAll(c: Collection<T>): Boolean = false
    override fun get(index: Int): T = v
    override fun indexOf(o: T): Int = -1
    override fun lastIndexOf(o: T): Int = -1
    override fun listIterator(): ListIterator<T> = throw Error()
    override fun listIterator(index: Int): ListIterator<T> = throw Error()
    override fun subList(fromIndex: Int, toIndex: Int): List<T> = throw Error()
    override fun hashCode(): Int = 0
    override fun equals(other: Any?): Boolean = false
}

fun box(): String {
    val list = MyList<String>("") as java.util.List<String>

    list.add("")
    list.remove("")
    list.addAll(list)
    list.removeAll(list)
    list.retainAll(list)
    list.clear()
    list.set(0, "")
    list.add(0, "")
    list.remove(0)

    return "OK"
}
