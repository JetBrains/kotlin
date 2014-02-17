class JavaClass {
    void testMethod() {
        _DefaultPackage.none();

        try {
            _DefaultPackage.one();
        }
        catch (E1 e) {}

        try {
            _DefaultPackage.two();
        }
        catch (E1 e) {}
        catch (E2 e) {}
    }
}