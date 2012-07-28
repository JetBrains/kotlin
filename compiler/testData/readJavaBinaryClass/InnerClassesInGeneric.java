package test;

import java.lang.UnsupportedOperationException;

public class InnerClassesInGeneric<P, Q> {
    public class Inner {
    }
    
    public class Inner2 extends Inner implements Iterable<P> {
        public java.util.Iterator<P> iterator() {
            throw new UnsupportedOperationException();
        }
    }
}
