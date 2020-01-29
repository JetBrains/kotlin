abstract class A<T : Any> : MutableCollection<T> {
    override fun contains(o: T): Boolean {
        throw UnsupportedOperationException()
    }
}

// 1 bridge
// 1 public final bridge size
// 0 INSTANCEOF

/* Only 1 null check should be within the contains method (because T is not nullable) */
// JVM_TEMPLATES:
// 1 IFNULL
// JVM_IR_TEMPLATES:
// 1 IFNONNULL
