package test;

public interface CustomAnnotationWithDefaultParameter {

    @MyAnnotation(first = "f", second = "s")
    public class MyTest {}

    public @interface MyAnnotation {
        String first();
        String second() default("s");
    }
}
