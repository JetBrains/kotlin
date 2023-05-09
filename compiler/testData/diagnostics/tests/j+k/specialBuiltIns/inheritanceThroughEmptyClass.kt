// FIR_IDENTICAL
// FULL_JDK
// ISSUE: KT-57693

// FILE: AbstractCollectionDecorator.java
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

public abstract class AbstractCollectionDecorator<E> implements Collection<E> {

    protected Collection<E> decorated() {
        return null;
    }

    public boolean add(E object) {
        return this.decorated().add(object);
    }

    public boolean addAll(Collection<? extends E> coll) {
        return this.decorated().addAll(coll);
    }

    public void clear() {
        this.decorated().clear();
    }

    public boolean contains(Object object) {
        return this.decorated().contains(object);
    }

    public boolean isEmpty() {
        return this.decorated().isEmpty();
    }

    public Iterator<E> iterator() {
        return this.decorated().iterator();
    }

    public boolean remove(Object object) {
        return this.decorated().remove(object);
    }

    public int size() {
        return this.decorated().size();
    }

    public Object[] toArray() {
        return this.decorated().toArray();
    }

    public <T> T[] toArray(T[] object) {
        return this.decorated().toArray(object);
    }

    public boolean containsAll(Collection<?> coll) {
        return this.decorated().containsAll(coll);
    }

    public boolean removeIf(Predicate<? super E> filter) {
        return this.decorated().removeIf(filter);
    }

    public boolean removeAll(Collection<?> coll) {
        return this.decorated().removeAll(coll);
    }

    public boolean retainAll(Collection<?> coll) {
        return this.decorated().retainAll(coll);
    }

    public String toString() {
        return this.decorated().toString();
    }
}

// FILE: AbstractSerializableListDecorator.java
public abstract class AbstractSerializableListDecorator<E> extends AbstractCollectionDecorator<E> {
}

// FILE: main.kt
import java.util.*

class UniqueArrayList<E> : AbstractSerializableListDecorator<E>(), MutableSet<E>
