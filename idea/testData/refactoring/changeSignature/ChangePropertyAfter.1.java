import org.jetbrains.annotations.NotNull;

import java.lang.Override;

class J extends A {
    private int p;

    @NotNull
    @Override
    public String getS() {
        return p;
    }

    @Override
    public void setS(@NotNull String value) {
        p = value;
    }
}

class Test {
    static void test() {
        new A().getS();
        new A().setS(1);

        new B().getS();
        new B().setS(2);

        new J().getS();
        new J().setS(3);
    }
}