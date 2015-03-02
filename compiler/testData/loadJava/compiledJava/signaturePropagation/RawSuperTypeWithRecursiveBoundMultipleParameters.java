package test;

import java.lang.Object;
import java.lang.Override;
import java.lang.UnsupportedOperationException;

public class RawSuperTypeWithRecursiveBoundMultipleParameters {

    public interface Super<R, T extends Super<R, T>> {
        void foo(R r, T t);

        void dummy(); // To make it not SAM
    }

    public class Derived implements Super {
        public void foo(Object o, Object o1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void foo(Object r, Super t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dummy() {}
    }

}