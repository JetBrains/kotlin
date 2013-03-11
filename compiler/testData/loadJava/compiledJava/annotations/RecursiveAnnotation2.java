package test;

public interface RecursiveAnnotation2 {

    public @interface A {
        B value();
    }

    @A(@B("test"))
    public @interface B {
        String value();
    }
}
