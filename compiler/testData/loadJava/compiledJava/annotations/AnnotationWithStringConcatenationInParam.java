package test;

public interface AnnotationWithStringConcatenationInParam {
    public @interface Anno {
        String value();
    }

    @Anno("he" + "l" + "lo")
    public static class Class { }
}
