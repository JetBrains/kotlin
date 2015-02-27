package test;

class JavaClass {
    void testMethod(One instance) {
        try {
            new One(1);
        }
        catch (E1 e) {}

        try {
            new One(1, 0, null);
        }
        catch (E1 e) {}

        try {
            new One();
        }
        catch (E1 e) {}

        try {
            One.one$default(instance, 1, 1);
        }
        catch (E1 e) {}

        try {
            TestPackage.one(1);
        }
        catch (E1 e) {}

        try {
            TestPackage.one$default(1, 0);
        }
        catch (E1 e) {}
    }
}
