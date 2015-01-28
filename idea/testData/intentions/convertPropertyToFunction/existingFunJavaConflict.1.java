import test.A;

class J extends A {
    boolean foo() {
        return true;
    }

    @Override
    int getFoo() {
        return 2;
    }
}