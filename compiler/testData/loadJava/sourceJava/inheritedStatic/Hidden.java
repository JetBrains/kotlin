package test;

public interface Hidden {
    class A {
        public static final int PUBLIC = 1;
    }

    class B extends A {
        public static final String PUBLIC = "";
    }
}