public abstract class JavaClass {
    public static String test() {
        return Test.INSTANCE.foo(new Outer<String>("OK").new Inner<Integer>(1));
    }
}
