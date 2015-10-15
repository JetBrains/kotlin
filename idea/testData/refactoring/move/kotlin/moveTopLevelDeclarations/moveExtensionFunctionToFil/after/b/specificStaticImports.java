package b;

import static b.DependencyKt.test;

class J {
    void bar() {
        test(new a.Test());
    }
}
