package b;

import a.*;

class J {
    void bar() {
        b.BPackage.getTest(new Test());
        b.BPackage.setTest(new Test(), 0);
    }
}
