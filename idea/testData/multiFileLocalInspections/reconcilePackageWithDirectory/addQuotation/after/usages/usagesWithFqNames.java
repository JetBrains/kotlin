package usages;

import in.foo.fun.Foo;
import in.foo.fun.TestKt;

class Test {
    static void test() {
        new Foo();
        TestKt.foo();
    }
}