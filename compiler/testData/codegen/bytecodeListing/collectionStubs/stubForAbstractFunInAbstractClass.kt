abstract class AbstractAdd {
    abstract fun add(s: String): Boolean
}

abstract class AbstractStringCollection : AbstractAdd(), Collection<String>
