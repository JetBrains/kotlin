package test;

public class AnnotatedConstructor {
    public static @interface Anno {
        String value();
    }

    @Anno("constructor")
    public AnnotatedConstructor() { }
}
