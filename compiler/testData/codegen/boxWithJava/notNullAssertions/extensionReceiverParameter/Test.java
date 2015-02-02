public class Test {
    public static String invokeFoo() {
        try {
            _DefaultPackage.foo(null);
        }
        catch (IllegalArgumentException e) {
            try {
                _DefaultPackage.getBar(null);
            }
            catch (IllegalArgumentException f) {
                return "OK";
            }
        }

        return "Fail: assertion must have been fired";
    }
}
