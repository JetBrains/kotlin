// IGNORE_BACKEND_FIR: JVM_IR

abstract class AbstractAdd {
    abstract fun add(s: String): Any
}

abstract class AbstractStringCollection : AbstractAdd(), Collection<String>

class StringCollection : AbstractStringCollection() {
    override fun add(s: String) = s

    override val size: Int get() = TODO()
    override fun contains(element: String): Boolean = TODO()
    override fun containsAll(elements: Collection<String>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<String> = TODO()
}

fun test1(a: AbstractAdd) =
    a.add("O") as String

fun test2(a: AbstractStringCollection) =
    a.add("K") as String

fun box() =
    test1(StringCollection()) + test2(StringCollection())