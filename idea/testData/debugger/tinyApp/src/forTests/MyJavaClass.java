package forTests;

import org.jetbrains.annotations.NotNull;

public class MyJavaClass {
    public void testFun() {
       int i = 1;
    }

    @NotNull
    public String testNotNullFun() {
       return "a";
    }

    public static int staticFun(Object s) {
        return 1;
    }

    public static <T> T runReadAction(@NotNull Computable<T> computation) {
        return computation.compute();
    }

    private static class PrivateJavaClass {
        public final int prop = 1;
    }
}
