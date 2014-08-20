package a;

import static b.BPackage.getTest;
import static b.BPackage.setTest;

class J {
    void bar() {
        getTest(new Test());
        setTest(new Test(), 0);
    }
}
