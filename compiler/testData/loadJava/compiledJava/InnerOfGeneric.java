package test;

import java.util.Iterator;

public class InnerOfGeneric {
    public interface S<E> {
       Iterator<E> iterator();
    }

    public abstract class A<K> {
        public abstract class Inner implements S<K> {
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
