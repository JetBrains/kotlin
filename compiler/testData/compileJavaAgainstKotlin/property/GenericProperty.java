package test;

class GenericProperty {
    void foo() {
        java.util.Map<?, ?> o = TestPackage.getTest();
    }
}
