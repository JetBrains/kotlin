/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm.internal.pcollections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A simple persistent stack of non-null values.
 * <p/>
 * This implementation is thread-safe, although its iterators may not be.
 */
final class ConsPStack<E> implements Iterable<E> {
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
        if (index < 0 || index > size) throw new IndexOutOfBoundsException();

        try {
            return iterator(index).next();
        } catch (NoSuchElementException e) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return iterator(0);
    }

    public int size() {
        return size;
    }

    private Iterator<E> iterator(final int index) {
        return new Iterator<E>() {
            ConsPStack<E> next = subList(index);

            @Override
            public boolean hasNext() {
                return next.size > 0;
            }

            @Override
            public E next() {
                E e = next.first;
                next = next.rest;
                return e;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public ConsPStack<E> plus(E e) {
        return new ConsPStack<E>(e, this);
    }

    private ConsPStack<E> minus(Object e) {
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

    private ConsPStack<E> subList(int start) {
        if (start < 0 || start > size)
            throw new IndexOutOfBoundsException();
        if (start == 0)
            return this;
        return rest.subList(start - 1);
    }
}
