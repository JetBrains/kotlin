abstract class AbstractAdd {
    abstract fun add(s: String): Any
}

abstract class AbstractStringCollection : AbstractAdd(), Collection<String>
