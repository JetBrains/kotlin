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

// 0 signature \(TQ;\)Z
// 2 signature \(Ljava/util/Collection<\+Ljava/lang/Object;>;\)Z
// 1 public final bridge contains\(Ljava/lang/Object;\)Z
// 1 public final bridge remove\(Ljava/lang/Object;\)Z
// 1 INVOKEVIRTUAL A[0-9]\.contains \(Ljava/lang/String;\)Z
/* 2 INSTANCEOF: one for 'remove', one for 'contains' type-safe bridges */
// 2 INSTANCEOF
