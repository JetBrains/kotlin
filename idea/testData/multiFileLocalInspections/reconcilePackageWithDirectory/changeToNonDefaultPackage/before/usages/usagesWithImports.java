package usages;

import source.Foo;
import static source.TestKt.foo;

class Test {
    static void test() {
        new Foo();
        foo();
    }
}