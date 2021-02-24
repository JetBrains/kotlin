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

// 0 signature \(TW;\)Z
// 2 signature \(Ljava/util/Collection<\+Ljava/lang/Object;>;\)Z
// 2 public final bridge contains\(Ljava/lang/Object;\)Z
// 2 public final bridge remove\(Ljava/lang/Object;\)Z
// 2 public final bridge indexOf\(Ljava/lang/Object;\)I
// 2 public final bridge lastIndexOf\(Ljava/lang/Object;\)I
/* 2 INSTANCEOF for each class: one for 'remove', one for 'contains' type-safe bridges */
// 8 INSTANCEOF java/lang/String
