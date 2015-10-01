import test.*;

class JA extends A {
    public JA() {
        super(1);
    }

    @Override
    boolean getFoo() {
        return true;
    }
}

class JTest {
    void test() {
        boolean x = new A(1).getFoo();
        boolean y = new JA().getFoo();
    }
}