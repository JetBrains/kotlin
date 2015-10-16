package c;

import static a.MainKt.*;

class J {
    void bar() {
        b.DependencyKt.getTest(new a.Test());
        b.DependencyKt.setTest(new a.Test(), 0);
    }
}
