package test;

import java.lang.Object;
import java.lang.Override;
import java.lang.UnsupportedOperationException;

public class RawSuperTypeWithBound {

    public interface Bound {}

    public interface Super<T extends Bound> {
        void foo(T t);

        void dummy(); // To make it not SAM
    }

    public class Derived implements Super {
        public void foo(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void foo(Bound o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dummy() {}
    }

}