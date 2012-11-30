package test

public trait SubclassOfCollection<E>: MutableCollection<E> {
    override fun iterator() : MutableIterator<E>
}
