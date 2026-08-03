// SKIP_IN_RUNTIME_TEST
// ^ There's no stable way to determine if a field is initialized with a non-null value in runtime.

package test;

public interface StringConstantInParam {
    public static final String HEL = "hel";

    public @interface Anno {
        String value();
    }

    @Anno(HEL + "lo")
    public static class Class { }
}
