package test;

public class ClassObjectInParam {
    public @interface Anno {
        Class<?> value();
    }

    @Anno(ClassObjectInParam.class)
    public static class Nested {}
}
