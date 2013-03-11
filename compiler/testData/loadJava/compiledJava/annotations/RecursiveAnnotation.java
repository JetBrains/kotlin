package test;

public interface RecursiveAnnotation {

    @B(@A("test"))
    public @interface A {
        String value();
    }

    @B(@A("test"))
    public @interface B {
        A value();
    }
}
