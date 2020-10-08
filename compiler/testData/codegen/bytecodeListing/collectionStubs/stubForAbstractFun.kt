abstract class AbstractAdd {
    abstract fun add(s: String): Boolean
}

class StringCollection : AbstractAdd(), Collection<String> {
    override fun add(s: String) = false

    override val size: Int get() = TODO()
    override fun contains(element: String): Boolean = TODO()
    override fun containsAll(elements: Collection<String>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<String> = TODO()
}