public class Test {
    public static String invokeMethodWithOverloads() {
        C<String> c = new C<String>();
        return c.foo("O");
    }
}