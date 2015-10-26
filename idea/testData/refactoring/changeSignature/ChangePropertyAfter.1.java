import org.jetbrains.annotations.NotNull;

import java.lang.Override;

public class J extends A {
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
        new A().setS(3);

        new AA().getS();
        new AA().setS(3);

        new J().getS();
        new J().setS(3);

        new B().getS();
        new B().setS(3);
    }
}