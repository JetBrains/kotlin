package test;

public interface AnnotationWithStringConstantInParam {
    public static final String HEL = "hel";

    public @interface Anno {
        String value();
    }

    @Anno(HEL + "lo")
    public static class Class { }
}
