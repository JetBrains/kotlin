package test;

public interface HiddenByPrivateInSuperClass {
    class Base {
        public static final int HIDDEN = 1;
    }

    class Derived extends base.Base {
        private static final String HIDDEN = "";
    }

    class Derived1 extends Derived {
    }
}