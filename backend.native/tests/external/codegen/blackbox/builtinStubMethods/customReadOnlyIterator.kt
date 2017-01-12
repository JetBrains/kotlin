class A : Collection<Char> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun contains(element: Char): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsAll(elements: Collection<Char>): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator() = MyIterator
}

object MyIterator : Iterator<Char> {
    override fun hasNext() = true

    override fun next() = 'a'
}


fun box(): String {
    val it: MyIterator = A().iterator()

    if (!it.hasNext()) return "fail 1"
    if (it.next() != 'a') return "fail 2"

    return "OK"
}
