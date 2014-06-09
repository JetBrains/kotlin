package kotlin.reflect.jvm.internal.pcollections;

/**
 * An immutable, persistent stack.
 *
 * @author harold
 */
public interface PStack<E> extends Iterable<E> {
    PStack<E> plus(E e);

    PStack<E> minus(int i);

    int size();
}
