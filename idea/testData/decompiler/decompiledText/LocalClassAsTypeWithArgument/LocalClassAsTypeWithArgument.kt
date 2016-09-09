package test

class LocalClassAsTypeWithArgument<E> {
    // anonymous type captures type parameter E of containing class
    private val z = object : Iterable<E> {
        override fun iterator() = null!!
    }
}
