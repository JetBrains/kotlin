import test.TestPackage;

class Test {
    static void test() {
        TestPackage.getP(new A());
        TestPackage.setP(new A(), 1);
    }
}