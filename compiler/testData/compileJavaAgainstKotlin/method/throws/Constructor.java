package test;

class JavaClass {
    void testMethod() {
        new None();

        try {
            new One();
        }
        catch (E1 e) {}

        try {
            new OneWithParam(1);
        }
        catch (E1 e) {}

        try {
            new Two();
        }
        catch (E1 e) {}
        catch (E2 e) {}
    }
}
