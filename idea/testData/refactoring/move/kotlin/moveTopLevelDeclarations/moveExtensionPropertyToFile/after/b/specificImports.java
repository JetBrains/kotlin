package b;

import a.Test;

class J {
    void bar() {
        DependencyKt.getTest(new Test());
        DependencyKt.setTest(new Test(), 0);
    }
}
