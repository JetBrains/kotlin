package test;

class JavaClass {
    void testMethod() {
        Test test = new Test();
        test.none();

        try {
            test.one();
        }
        catch (E1 e) {}

        try {
            test.two();
        }
        catch (E1 e) {}
        catch (E2 e) {}
    }
}
