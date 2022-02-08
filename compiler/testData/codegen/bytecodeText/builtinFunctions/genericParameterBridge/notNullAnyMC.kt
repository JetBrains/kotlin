abstract class A8 : MutableCollection<Any> {
    override fun contains(o: Any): Boolean {
        throw UnsupportedOperationException()
    }
}

// 1 bridge
// 1 public final bridge size
// 0 INSTANCEOF

/* Only 1 null check should be within the contains method */
// 1 IFNONNULL
