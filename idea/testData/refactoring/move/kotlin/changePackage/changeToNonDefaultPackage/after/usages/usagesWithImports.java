package usages;

import target.Foo;
import static target.TargetPackage.foo;

class Test {
    static void test() {
        new Foo();
        foo();
    }
}