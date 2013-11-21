package test;

public interface AmbiguityHiddenByPrivateInSuperclass {
    class Base {
        public static final int HIDDEN = 1;
    }

    class Derived extends Base {
        private static final String HIDDEN = "";
    }

    interface Interface {
        long HIDDEN = 1L;
    }

    class Derived1 extends Derived implements Interface {
    }
}