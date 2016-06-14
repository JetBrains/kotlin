package usage;

import lib.Foo;

public class JUsage implements Foo {
    @Override
    public int getFoo() {
        return 1;
    }

    @Override
    public void /*rename*/setFoo(int value) {
    }
}