package test;

public interface AmbiguityHiddenByExplicitDeclaration {

    class Base {
        public static final int HIDDEN = 1;
    }

    interface Interface {
        long HIDDEN = 1L;
    }

    class Derived extends Base implements Interface {
        public static final String HIDDEN = "";
    }
}