package a;

import static a.MainKt.*;

class J {
    void bar() {
        b.DependencyKt.getTest(new Test());
        b.DependencyKt.setTest(new Test(), 0);
    }
}
