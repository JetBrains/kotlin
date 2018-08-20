// JAVAC_OPTIONS: -parameters

package test;

public class ParameterNames {
    public ParameterNames(long longConstructorParam, String stringConstructorParam) {}

    public void foo(long longMethodParam, int intMethodParam) {}

    static void bar(ParameterNames staticMethodParam) {}

    class Inner {
        public Inner(double doubleInnerParam, Object objectInnerParam) {}
    }

    enum Enum {
        E(0.0, "");

        Enum(double doubleEnumParam, String stringEnumParam) {}
    }
}
