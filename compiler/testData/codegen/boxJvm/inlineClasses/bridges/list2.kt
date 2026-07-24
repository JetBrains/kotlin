// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@JvmInline
value class MyInlineClass(val all: List<String>) : List<String> {
    override val size: Int
        get() = all.size

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun contains(element: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<String> {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): String = all[index]

    override fun indexOf(element: String): Int {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: String): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): ListIterator<String> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<String> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<String> {
        TODO("Not yet implemented")
    }
}

fun box(): String {
    val c: List<String> = MyInlineClass(listOf("a"))
    if (c[0] != "a") return "FAIL: ${c[0]}"
    return "OK"
}

