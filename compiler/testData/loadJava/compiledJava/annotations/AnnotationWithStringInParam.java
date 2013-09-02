package test;

public interface AnnotationWithStringInParam {
    public @interface Anno {
        String value();
    }

    @Anno("hello")
    public static class Class { }
}
