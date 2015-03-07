package test;

public class AnnotatedMethod {
    public static @interface Anno {
        int value();
    }

    @Anno(42)
    public void f() { }
}
