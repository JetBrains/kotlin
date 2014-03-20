package test;

class JavaClass {
    void testMethod() {
        TestPackage.none();

        try {
            TestPackage.one();
        }
        catch (E1 e) {}

        try {
            TestPackage.two();
        }
        catch (E1 e) {}
        catch (E2 e) {}
    }
}
