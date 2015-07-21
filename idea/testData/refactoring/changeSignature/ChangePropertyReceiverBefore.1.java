import java.lang.Override;

class J extends A {
    private int p;

    @Override
    public int getP(String receiver) {
        return p;
    }

    @Override
    public void setP(String receiver, int value) {
        p = value;
    }
}

class Test {
    static void test() {
        new A().getP("");
        new A().setP("", 1);

        new B().getP("");
        new B().setP("", 2);

        new J().getP("");
        new J().setP("", 3);
    }
}