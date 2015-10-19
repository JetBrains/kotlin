package test;

class JavaClass {
    void testMethod() {
        TopLevelKt.none();

        try {
            TopLevelKt.one();
        }
        catch (E1 e) {}

        try {
            TopLevelKt.two();
        }
        catch (E1 e) {}
        catch (E2 e) {}
    }
}
