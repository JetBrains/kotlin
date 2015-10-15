package c;

import a.MainKt;
import a.Test;

class J {
    void bar() {
        MainKt.getTest(new Test());
        MainKt.setTest(new Test(), 0);
    }
}
