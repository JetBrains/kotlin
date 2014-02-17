package test;

import org.jetbrains.annotations.NotNull;

public abstract class doGenerateParamAssertions<Type> {

    public abstract void doTest(@NotNull Type s);
    
    public static void runTest(doGenerateParamAssertions a) {
        try {
            a.doTest(null);
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("Fail: IllegalArgumentException expected");
    }
}
