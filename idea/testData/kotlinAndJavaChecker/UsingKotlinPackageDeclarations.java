import test.TestPackage;

// Check absence of 'Static method reference via subclass warning' for kotlin usages
public class UsingKotlinPackageDeclarations {
    public static int test() {
        TestPackage.foo();
        TestPackage.setBar(15);
        return TestPackage.getBar();
    }
}