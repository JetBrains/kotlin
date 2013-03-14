package test;

public enum Enum {
    A,
    B,
    C;

    public static class Nested {
        void foo() {}
        void values() {}
    }

    public class Inner {
        void bar() {}
        void valueOf(String s) {}
    }
}