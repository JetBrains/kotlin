// JVM_DEFAULT_MODE: enable

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

abstract class A : I2

// 1 public final bridge contains\(Ljava/lang/Object;\)Z
// 1 public final bridge remove\(Ljava/lang/Object;\)Z

// There are 3 instanceof instructions:
// * one in the bridge 'A.remove'
// * one in the bridge 'A.contains'
// * one in the bridge 'I2.contains' -- this one is unnecessary, see KT-75239
// 3 INSTANCEOF java/lang/String
