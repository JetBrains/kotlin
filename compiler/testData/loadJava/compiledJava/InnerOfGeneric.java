package test;

import java.util.Iterator;

public class InnerOfGeneric {
    public class S<E> {
        public Iterator<E> iterator() { return null; }
    }

    public abstract class A<K> {
        public abstract class Inner extends S<K> {
        }
    }

    public class B<L> extends A<L> {
        public class SubInner extends Inner {
            @Override
            public Iterator<L> iterator() {
                throw new RuntimeException();
            }
        }
    }
}
