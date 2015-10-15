package test;

// Check absence of 'Static method reference via subclass warning' for kotlin usages
public class UsingKotlinPackageDeclarations {
    public static int test() {
        UsingKotlinPackageDeclarationsKt.foo();
        UsingKotlinPackageDeclarationsKt.setBar(15);
        return UsingKotlinPackageDeclarationsKt.getBar();
    }
}