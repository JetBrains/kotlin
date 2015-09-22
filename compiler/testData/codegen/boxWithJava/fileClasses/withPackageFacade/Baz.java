import test.TestPackage;

public class Baz {
    public static String baz() {
        return TestPackage.foo() + TestPackage.bar();
    }
}