// IGNORE_BACKEND_FIR: JVM_IR
open class A1 {
    open val size: Int = 56
}

class A2 : A1(), Collection<String> {
    // No 'getSize()' method should be generated in A2

    override fun contains(element: String): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsAll(elements: Collection<String>): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<String> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun box(): String {
    if (A2().size != 56) return "fail 1"
    return "OK"
}
