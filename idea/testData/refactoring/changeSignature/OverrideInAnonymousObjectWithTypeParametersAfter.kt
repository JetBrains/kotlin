interface X

interface A<D> {
    public fun <caret>bar(receiverTypes: Collection<X>): Collection<D>
}

fun foo<D>(): A<D> {
    return object: A<D> {
        override fun bar(receiverTypes: Collection<X>): Collection<D> {
            throw UnsupportedOperationException()
        }
    }
}