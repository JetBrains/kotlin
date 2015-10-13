public class Test {
    public static String invokeMethodWithPublicField() {
        C c = new C();
        return c.foo;
    }
}