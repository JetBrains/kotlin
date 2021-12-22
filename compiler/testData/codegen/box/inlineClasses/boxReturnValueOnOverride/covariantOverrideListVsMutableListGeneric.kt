// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IFooList {
    fun foo(): List<String>
}

interface IFooMutableList {
    fun foo(): MutableList<String>
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class AL<T: MutableList<String>>(val t: T) : MutableList<String> {
    override val size: Int get() = t.size
    override fun get(index: Int): String = t.get(index)
    override fun set(index: Int, element: String): String = t.set(index, element)
    override fun contains(element: String): Boolean = t.contains(element)
    override fun containsAll(elements: Collection<String>): Boolean = t.containsAll(elements)
    override fun indexOf(element: String): Int = t.indexOf(element)
    override fun isEmpty(): Boolean = t.isEmpty()
    override fun iterator(): MutableIterator<String> = t.iterator()
    override fun lastIndexOf(element: String): Int = t.lastIndexOf(element)
    override fun add(element: String): Boolean = t.add(element)
    override fun add(index: Int, element: String) = t.add(index, element)
    override fun addAll(index: Int, elements: Collection<String>): Boolean = t.addAll(index, elements)
    override fun addAll(elements: Collection<String>): Boolean = t.addAll(elements)
    override fun listIterator(): MutableListIterator<String> = t.listIterator()
    override fun listIterator(index: Int): MutableListIterator<String> = t.listIterator(index)
    override fun clear() { t.clear() }
    override fun remove(element: String): Boolean = t.remove(element)
    override fun removeAll(elements: Collection<String>): Boolean = t.removeAll(elements)
    override fun removeAt(index: Int): String = t.removeAt(index)
    override fun retainAll(elements: Collection<String>): Boolean = t.retainAll(elements)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<String> = t.subList(fromIndex, toIndex)
}

class Test : IFooList, IFooMutableList {
    val arr = arrayListOf<String>()
    override fun foo() = AL(arr)
}

fun box(): String {
    val t1: IFooList = Test()
    val list1 = t1.foo()
    if (list1 !is AL<*>) throw AssertionError("list1: $list1")

    val t2: IFooMutableList = Test()
    val list2 = t2.foo()
    if (list2 !is AL<*>) throw AssertionError("list2: $list2")

    return "OK"
}