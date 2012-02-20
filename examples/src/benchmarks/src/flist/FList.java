/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package flist;

import java.util.NoSuchElementException;

abstract public class FList<T> {
    abstract T getHead();

    abstract FList<T> getTail();
    
    abstract FList<T> plus(T element);
    
    static class Empty<T> extends FList<T> {

        @Override
        T getHead() {
            throw new NoSuchElementException();
        }

        @Override
        FList<T> getTail() {
            throw new NoSuchElementException();
        }

        @Override
        FList<T> plus(T element) {
            return new OneElementList<T>(element);
        }
    }

    static class OneElementList<T> extends FList<T> {
        private T element;

        public OneElementList(T element) {
            super();
            this.element = element;
        }

        @Override
        T getHead() {
            return element;
        }

        @Override
        FList<T> getTail() {
            return new Empty<T>();
        }

        @Override
        FList<T> plus(T element) {
            return new StandardList<T>(element, this);
        }
    }

    private static class StandardList<T> extends FList<T> {
        private T head;
        private FList<T> tail;

        public StandardList(T head, FList<T> tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        T getHead() {
            return head;
        }

        @Override
        FList<T> getTail() {
            return tail;
        }

        @Override
        FList<T> plus(T element) {
            return new StandardList<T>(element, this);
        }
    }

    public static void main(String[] args) {
        for(int k = 0; k != 10; ++k) {
            long start = System.currentTimeMillis();
            FList<Integer> list = new Empty();
            for(int i = 0; i != 5000000; ++i)
                list = list.plus(i);
            System.out.println(System.currentTimeMillis()-start);
        }
    }
}
