package test;

public interface StringInParam {
    public @interface Anno {
        String value();
    }

    @Anno("hello")
    public static class Class { }
}
