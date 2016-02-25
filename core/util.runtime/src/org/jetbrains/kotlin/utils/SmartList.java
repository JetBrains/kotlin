/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.utils;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;

/**
 * A List which is optimised for the sizes of 0 and 1,
 * in which cases it would not allocate array at all.
 *
 * This class was copied from com.intellij.util.SmartList.
 */
@SuppressWarnings("unchecked")
public class SmartList<E> extends AbstractList<E> implements RandomAccess {
    private int mySize;
    private Object myElem; // null if mySize==0, (E)elem if mySize==1, Object[] if mySize>=2

    public SmartList() { }

    public SmartList(E element) {
        add(element);
    }

    public SmartList(@NotNull Collection<? extends E> elements) {
        int size = elements.size();
        if (size == 1) {
            E element = elements instanceof List ? (E)((List)elements).get(0) : elements.iterator().next();
            add(element);
        }
        else if (size > 0) {
            mySize = size;
            myElem = elements.toArray(new Object[size]);
        }
    }

    public SmartList(@NotNull E... elements) {
        if (elements.length == 1) {
            add(elements[0]);
        }
        else if (elements.length > 0) {
            mySize = elements.length;
            myElem = Arrays.copyOf(elements, mySize);
        }
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= mySize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mySize);
        }
        if (mySize == 1) {
            return (E)myElem;
        }
        return (E)((Object[])myElem)[index];
    }

    @Override
    public boolean add(E e) {
        if (mySize == 0) {
            myElem = e;
        }
        else if (mySize == 1) {
            Object[] array = new Object[2];
            array[0] = myElem;
            array[1] = e;
            myElem = array;
        }
        else {
            Object[] array = (Object[])myElem;
            int oldCapacity = array.length;
            if (mySize >= oldCapacity) {
                // have to resize
                int newCapacity = oldCapacity * 3 / 2 + 1;
                int minCapacity = mySize + 1;
                if (newCapacity < minCapacity) {
                    newCapacity = minCapacity;
                }
                Object[] oldArray = array;
                myElem = array = new Object[newCapacity];
                System.arraycopy(oldArray, 0, array, 0, oldCapacity);
            }
            array[mySize] = e;
        }

        mySize++;
        modCount++;
        return true;
    }

    @Override
    public void add(int index, E e) {
        if (index < 0 || index > mySize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mySize);
        }

        if (mySize == 0) {
            myElem = e;
        }
        else if (mySize == 1 && index == 0) {
            Object[] array = new Object[2];
            array[0] = e;
            array[1] = myElem;
            myElem = array;
        }
        else {
            Object[] array = new Object[mySize + 1];
            if (mySize == 1) {
                array[0] = myElem; // index == 1
            }
            else {
                Object[] oldArray = (Object[])myElem;
                System.arraycopy(oldArray, 0, array, 0, index);
                System.arraycopy(oldArray, index, array, index + 1, mySize - index);
            }
            array[index] = e;
            myElem = array;
        }

        mySize++;
        modCount++;
    }

    @Override
    public int size() {
        return mySize;
    }

    @Override
    public void clear() {
        myElem = null;
        mySize = 0;
        modCount++;
    }

    @Override
    public E set(int index, E element) {
        if (index < 0 || index >= mySize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mySize);
        }

        E oldValue;
        if (mySize == 1) {
            oldValue = (E)myElem;
            myElem = element;
        }
        else {
            Object[] array = (Object[])myElem;
            oldValue = (E)array[index];
            array[index] = element;
        }
        return oldValue;
    }

    @Override
    public E remove(int index) {
        if (index < 0 || index >= mySize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mySize);
        }

        E oldValue;
        if (mySize == 1) {
            oldValue = (E)myElem;
            myElem = null;
        }
        else {
            Object[] array = (Object[])myElem;
            oldValue = (E)array[index];

            if (mySize == 2) {
                myElem = array[1 - index];
            }
            else {
                int numMoved = mySize - index - 1;
                if (numMoved > 0) {
                    System.arraycopy(array, index + 1, array, index, numMoved);
                }
                array[mySize - 1] = null;
            }
        }
        mySize--;
        modCount++;
        return oldValue;
    }

    private static class EmptyIterator<T> implements Iterator<T> {
        private static final EmptyIterator INSTANCE = new EmptyIterator();
        public static <T> EmptyIterator<T> getInstance() {
            //noinspection unchecked
            return INSTANCE;
        }
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new IllegalStateException();
        }
    }


    @NotNull
    @Override
    public Iterator<E> iterator() {
        if (mySize == 0) {
            return EmptyIterator.getInstance();
        }
        if (mySize == 1) {
            return new SingletonIterator();
        }
        return super.iterator();
    }


    private abstract static class SingletonIteratorBase<T> implements Iterator<T> {
        private boolean myVisited;

        @Override
        public final boolean hasNext() {
            return !myVisited;
        }

        @Override
        public final T next() {
            if (myVisited) {
                throw new NoSuchElementException();
            }
            myVisited = true;
            checkCoModification();
            return getElement();
        }

        protected abstract void checkCoModification();

        protected abstract T getElement();
    }

    private class SingletonIterator extends SingletonIteratorBase<E> {
        private final int myInitialModCount;

        public SingletonIterator() {
            myInitialModCount = modCount;
        }

        @Override
        protected E getElement() {
            return (E)myElem;
        }

        @Override
        protected void checkCoModification() {
            if (modCount != myInitialModCount) {
                throw new ConcurrentModificationException("ModCount: " + modCount + "; expected: " + myInitialModCount);
            }
        }

        @Override
        public void remove() {
            checkCoModification();
            clear();
        }
    }

    public void sort(Comparator<? super E> comparator) {
        if (mySize >= 2) {
            Arrays.sort((E[])myElem, 0, mySize, comparator);
        }
    }

    public int getModificationCount() {
        return modCount;
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        int aLength = a.length;
        if (mySize == 1) {
            if (aLength != 0) {
                a[0] = (T)myElem;
            }
            else {
                T[] r = (T[])Array.newInstance(a.getClass().getComponentType(), 1);
                r[0] = (T)myElem;
                return r;
            }
        }
        else if (aLength < mySize) {
            return (T[])Arrays.copyOf((E[])myElem, mySize, a.getClass());
        }
        else if (mySize != 0) {
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(myElem, 0, a, 0, mySize);
        }

        if (aLength > mySize) {
            a[mySize] = null;
        }
        return a;
    }

    /**
     * Trims the capacity of this list to be the
     * list's current size.  An application can use this operation to minimize
     * the storage of a list instance.
     */
    public void trimToSize() {
        if (mySize < 2) return;
        Object[] array = (Object[])myElem;
        int oldCapacity = array.length;
        if (mySize < oldCapacity) {
            modCount++;
            myElem = Arrays.copyOf(array, mySize);
        }
    }
}
