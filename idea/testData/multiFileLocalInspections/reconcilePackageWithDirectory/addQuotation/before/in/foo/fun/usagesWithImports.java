package in.foo.fun;

import bar.Foo;
import static bar.TestKt.foo;

class Test {
    static void test() {
        new Foo();
        foo();
    }
}