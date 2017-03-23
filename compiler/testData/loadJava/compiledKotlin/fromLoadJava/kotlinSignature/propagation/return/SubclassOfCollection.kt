package test

public interface SubclassOfCollection<E>: MutableCollection<E> {
    override fun iterator() : MutableIterator<E>
}
