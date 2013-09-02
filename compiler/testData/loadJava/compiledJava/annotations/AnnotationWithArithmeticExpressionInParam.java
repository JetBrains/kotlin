package test;

public class AnnotationWithArithmeticExpressionInParam {
    public @interface Anno {
        int value();
    }

    @Anno(2 * 8 + 13 * (239 - 237))
    public static class Class {}
}
