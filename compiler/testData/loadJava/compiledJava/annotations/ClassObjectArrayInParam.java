package test;

public class ClassObjectArrayInParam {
    public @interface Anno {
        Class<?>[] value();
    }

    @Anno({ClassObjectArrayInParam.class, Nested.class, String.class})
    public static class Nested {}
}
