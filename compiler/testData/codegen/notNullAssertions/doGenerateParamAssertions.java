package test;

import org.jetbrains.annotations.NotNull;

public abstract class doGenerateParamAssertions {

    public abstract void bar(@NotNull String s);
    
    public static void foo(doGenerateParamAssertions a) {
        try {
            a.bar(null);
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("Fail: IllegalArgumentException expected");
    }
}
