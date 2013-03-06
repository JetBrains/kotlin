package test;

import java.lang.Object;
import java.lang.Override;
import java.lang.UnsupportedOperationException;

public class RawSuperType {

    public interface Super<T> {
        void foo(T t);
    }

    public class Derived implements Super {
        @Override
        public void foo(Object o) {
            throw new UnsupportedOperationException();
        }
    }

}