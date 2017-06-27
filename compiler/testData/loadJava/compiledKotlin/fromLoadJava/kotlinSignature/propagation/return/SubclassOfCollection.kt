// FULL_JDK
// JAVAC_EXPECTED_FILE

package test

public interface SubclassOfCollection<E>: MutableCollection<E> {
    override fun iterator() : MutableIterator<E>
}
