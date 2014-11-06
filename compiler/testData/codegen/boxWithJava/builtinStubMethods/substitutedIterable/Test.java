public class Test {
    public static void checkCallFromJava() {
        try {
            String x = _DefaultPackage.foo().iterator().next();
            throw new AssertionError("E should have been thrown");
        } catch (E e) { }
    }
}
