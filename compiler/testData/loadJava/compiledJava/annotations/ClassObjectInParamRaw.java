package test;

public class ClassObjectInParamRaw {
    public @interface Anno {
        Class value();
        Class[] arg();
    }

    @Anno(value = ClassObjectInParamRaw.class, arg = {})
    public static class Nested {}
}
