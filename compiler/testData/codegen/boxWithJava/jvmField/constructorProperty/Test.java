public class Test {
    public static String invokeMethodWithPublicField() {
        C c = new C("OK");
        return c.foo;
    }
}