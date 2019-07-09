// IGNORE_BACKEND: JVM_IR
abstract class A<T : Any> : MutableCollection<T> {
    override fun contains(o: T): Boolean {
        throw UnsupportedOperationException()
    }
}

// 1 bridge
// 1 public final bridge size
// 0 INSTANCEOF
/* Only 1 IFNULL should be within contains method (because T is not nullable) */
// 1 IFNULL
