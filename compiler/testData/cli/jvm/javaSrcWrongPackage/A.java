package foo;

public class A {
    public Nested nested() {
        return new Nested();
    }

    public static class Nested {}
}
