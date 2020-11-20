// IGNORE_BACKEND: NATIVE

interface Container {
    fun removeAt(x: Int): String
}

open class ContainerImpl : Container {
    override fun removeAt(x: Int) = "abc"
}

class A : ContainerImpl(), MutableList<String> {
    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun contains(element: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(index: Int): String {
        throw UnsupportedOperationException()
    }

    override fun indexOf(element: String): Int {
        throw UnsupportedOperationException()
    }

    override fun lastIndexOf(element: String): Int {
        throw UnsupportedOperationException()
    }

    override fun add(element: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(element: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(elements: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(index: Int, elements: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(elements: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(elements: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun set(index: Int, element: String): String {
        throw UnsupportedOperationException()
    }

    override fun add(index: Int, element: String) {
        throw UnsupportedOperationException()
    }

    override fun listIterator(): MutableListIterator<String> {
        throw UnsupportedOperationException()
    }

    override fun listIterator(index: Int): MutableListIterator<String> {
        throw UnsupportedOperationException()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<String> {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<String> {
        throw UnsupportedOperationException()
    }
}

fun box(): String {
    val a = A()
    if (a.removeAt(0) != "abc") return "fail 1"

    val l: MutableList<String> = a
    if (l.removeAt(0) != "abc") return "fail 2"

    val anyList: MutableList<Any?> = a as MutableList<Any?>
    if (anyList.removeAt(0) != "abc") return "fail 3"

    val container: Container = a
    if (container.removeAt(0) != "abc") return "fail 4"

    return "OK"
}
