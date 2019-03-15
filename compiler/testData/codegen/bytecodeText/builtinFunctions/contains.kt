abstract class A1<Q> : MutableCollection<Q> {
    override fun contains(o: Q): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<Q>): Boolean {
        throw UnsupportedOperationException()
    }
}

abstract class A2 : MutableCollection<String> {
    override fun contains(o: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }
}

abstract class A3<W> : java.util.AbstractList<W>()
abstract class A4<W> : java.util.AbstractList<W>() {
    override fun contains(o: W): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<W>): Boolean {
        throw UnsupportedOperationException()
    }
}

abstract class A5 : java.util.AbstractList<String>()
abstract class A6 : java.util.AbstractList<String>() {
    override fun contains(o: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }
}

interface I1<R> : MutableSet<R> {
    override fun contains(o: R): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<R>): Boolean {
        throw UnsupportedOperationException()
    }
}

interface I2 : MutableSet<String> {
    override fun contains(o: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsAll(c: Collection<String>): Boolean {
        throw UnsupportedOperationException()
    }
}

abstract class A7 : MutableCollection<Int> {
    override fun contains(o: Int): Boolean {
        throw UnsupportedOperationException()
    }
}

abstract class A8 : MutableCollection<Any?> {
    override fun contains(o: Any?): Boolean {
        throw UnsupportedOperationException()
    }
}


fun foo(
        a1: A1<String>,
        a2: A2,
        a3: A3<String>,
        a4: A4<String>,
        a5: A5,
        a6: A6,
        a7: A7,
        i1: I1<String>,
        i2: I2,
        c: Collection<String>
) {
    a1.contains("")
    a2.contains("")
    a3.contains("")
    a4.contains("")
    a5.contains("")
    a6.contains("")
    a7.contains(1)
    i1.contains("")
    i2.contains("")
    c.contains("")
}

/*
* Calls to a1-a7, i1-i2, c in foo
*/
// 7 INVOKEVIRTUAL A[0-9]\.contains \(Ljava/lang/Object;\)Z
// 1 INVOKEVIRTUAL A7\.contains \(I\)Z
// 1 INVOKEINTERFACE java/util/Collection.contains \(Ljava/lang/Object;\)Z
// 2 INVOKEINTERFACE I[1-2].contains \(Ljava/lang/Object;\)Z
