package test;

public class NestedEnumArgument {
    public enum E {
        FIRST
    }

    public @interface Anno {
        E value();
    }

    @Anno(E.FIRST)
    void foo() {}
}
