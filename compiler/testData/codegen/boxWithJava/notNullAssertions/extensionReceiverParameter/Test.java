public class Test {
    public static String invokeFoo() {
        try {
            ExtensionKt.foo(null);
        }
        catch (IllegalArgumentException e) {
            try {
                ExtensionKt.getBar(null);
            }
            catch (IllegalArgumentException f) {
                return "OK";
            }
        }

        return "Fail: assertion must have been fired";
    }
}
