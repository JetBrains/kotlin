// SKIP_IN_RUNTIME_TEST because there's no stable way to determine if a field is initialized with a non-null value in runtime

package test;

public class StaticFinal {
    public static final String publicNonNull = "aaa";
    public static final String publicNull = null;
    static final String packageNonNull = "bbb";
    static final String packageNull = null;
    private static final String privateNonNull = "bbb";
    private static final String privateNull = null;
}
