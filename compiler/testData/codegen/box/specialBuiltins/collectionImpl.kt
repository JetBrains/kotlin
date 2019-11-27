// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

class A1 : MutableCollection<String> {
    override val size: Int
        get() = 56

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun contains(o: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun iterator(): MutableIterator<String> {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun add(e: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(o: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun addAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun removeAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun retainAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        throw UnsupportedOperationException()
    }
}

class A2 : java.util.AbstractCollection<String>() {
    override val size: Int
        get() = 56

    override fun iterator(): MutableIterator<String> {
        throw UnsupportedOperationException()
    }
}

class A3 : java.util.ArrayList<String>() {
    override val size: Int
        get() = 56
}

interface Sized {
    val size: Int
}

class A4 : java.util.ArrayList<String>(), Sized {
    override val size: Int
        get() = 56
}

fun check56(x: Collection<String>) {
    if (x.size != 56) throw java.lang.RuntimeException("fail ${x.size}")
}

fun box(): String {
    val a1 = A1()
    if (a1.size != 56) return "fail 1: ${a1.size}"
    check56(a1)

    val a2 = A2()
    if (a2.size != 56) return "fail 2: ${a2.size}"
    check56(a2)

    val a3 = A3()
    if (a3.size != 56) return "fail 3: ${a3.size}"
    check56(a3)

    val a4 = A4()
    if (a4.size != 56) return "fail 4: ${a4.size}"
    check56(a4)

    val sized: Sized = a4
    if (sized.size != 56) return "fail 5: ${a4.size}"

    return "OK"
}
