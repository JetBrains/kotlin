package usage;

import lib.Foo;

public class JUsage implements Foo {
    @Override
    public int /*rename*/getFoo() {
        return 1;
    }

    @Override
    public void setFoo(int value) {
    }
}