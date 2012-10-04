package test;

import org.jetbrains.annotations.NotNull;

public abstract class doGenerateParamAssertions {

    public abstract void bar(@NotNull String s);
    
    public static String foo(doGenerateParamAssertions a) {
        try {
            a.bar(null);
        } catch (IllegalArgumentException e) {
            return "OK";
        }
        return "Fail: AssertionError expected";
    }
}
