package test;

public interface AnnotationWithAnnotationInParam {

    public @interface MyAnnotationWithParam {
        MyAnnotation value();
    }

    public @interface MyAnnotation {
        String value();
    }

    @MyAnnotationWithParam(@MyAnnotation("test"))
    public class A {}

    public @interface MyAnnotation2 {
        String[] value();
    }

    public @interface MyAnnotationWithParam2 {
        MyAnnotation2 value();
    }

    @MyAnnotationWithParam2(@MyAnnotation2({"test", "test2"}))
    public class B {}

    public @interface MyAnnotation3 {
        String first();
        String second();
    }

    public @interface MyAnnotationWithParam3 {
        MyAnnotation3 value();
    }

    @MyAnnotationWithParam3(@MyAnnotation3(first = "f", second = "s"))
    public class C {}
}