package c;

import static b.BPackage.getTest;
import static b.BPackage.setTest;

class J {
    void bar() {
        getTest(new a.Test());
        setTest(new a.Test(), 0);
    }
}
