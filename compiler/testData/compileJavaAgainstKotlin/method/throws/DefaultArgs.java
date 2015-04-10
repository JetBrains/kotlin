package test;

class JavaClass {
    void testMethod(One instance) {
        try {
            new One(1);
        }
        catch (E1 e) {}

        try {
            new One();
        }
        catch (E1 e) {}

        try {
            TestPackage.one(1);
        }
        catch (E1 e) {}

        try {
            TestPackage.one();
        }
        catch (E1 e) {}
    }
}
