package test;

public interface Visibility {
    class A {
        private static final int PRIVATE = 1;
        protected static final int PROTECTED = 1;
        /*package*/ static final int PACKAGE = 1;
        public static final int PUBLIC = 1;
    }

    class B extends A {}
}