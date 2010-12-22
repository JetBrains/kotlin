package org.jetbrains.jet.lang.parsing;

/**
 * @author abreslav
 */
public interface Consumer<T> {
    void consume(T item);
}
