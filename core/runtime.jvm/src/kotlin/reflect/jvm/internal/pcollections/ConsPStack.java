package kotlin.reflect.jvm.internal.pcollections;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A simple persistent stack of non-null values.
 * <p/>
 * This implementation is thread-safe, although its iterators may not be.
 *
 * @author harold
 */
public final class ConsPStack<E> implements Iterable<E> {
    private static final ConsPStack<Object> EMPTY = new ConsPStack<Object>();

    @SuppressWarnings("unchecked")
    public static <E> ConsPStack<E> empty() {
        return (ConsPStack<E>) EMPTY;
    }

    private final E first;
    private final ConsPStack<E> rest;
    private final int size;

    private ConsPStack() { // EMPTY constructor
        size = 0;
        first = null;
        rest = null;
    }

    private ConsPStack(E first, ConsPStack<E> rest) {
        this.first = first;
        this.rest = rest;
        this.size = 1 + rest.size;
    }

    public E get(int index) {
        try {
            return listIterator(index).next();
        } catch (NoSuchElementException e) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator(0);
    }

    public int size() {
        return size;
    }

    public ListIterator<E> listIterator(final int index) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException();

        return new ListIterator<E>() {
            int i = index;
            ConsPStack<E> next = subList(index);

            @Override
            public boolean hasNext() {
                return next.size > 0;
            }

            @Override
            public boolean hasPrevious() {
                return i > 0;
            }

            @Override
            public int nextIndex() {
                return index;
            }

            @Override
            public int previousIndex() {
                return index - 1;
            }

            @Override
            public E next() {
                E e = next.first;
                next = next.rest;
                return e;
            }

            @Override
            public E previous() {
                System.err.println("ConsPStack.listIterator().previous() is inefficient, don't use it!");
                next = subList(index - 1); // go from beginning...
                return next.first;
            }

            @Override
            public void add(E o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(E o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public ConsPStack<E> plus(E e) {
        return new ConsPStack<E>(e, this);
    }

    public ConsPStack<E> minus(Object e) {
        if (size == 0) return this;
        if (first.equals(e)) // found it
            return rest; // don't recurse (only remove one)
        // otherwise keep looking:
        ConsPStack<E> newRest = rest.minus(e);
        if (newRest == rest) return this;
        return new ConsPStack<E>(first, newRest);
    }

    public ConsPStack<E> minus(int i) {
        return minus(get(i));
    }

    public ConsPStack<E> subList(int start) {
        if (start < 0 || start > size)
            throw new IndexOutOfBoundsException();
        if (start == 0)
            return this;
        return rest.subList(start - 1);
    }
}
