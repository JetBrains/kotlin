package a;

import static a.MainKt.getTest;
import static a.MainKt.setTest;

class J {
    void bar() {
        getTest(new Test());
        setTest(new Test(), 0);
    }
}
