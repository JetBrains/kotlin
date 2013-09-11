package test;

public interface StringConcatenationInParam {
    public @interface Anno {
        String value();
    }

    @Anno("he" + "l" + "lo")
    public static class Class { }
}
