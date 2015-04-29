package usages;

import target.Foo;
import target.TargetPackage;

class Test {
    static void test() {
        new Foo();
        TargetPackage.foo();
    }
}