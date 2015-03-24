package b;

import a.*;

class J {
    void bar() {
        BPackage.getTest(new Test());
        BPackage.setTest(new Test(), 0);
    }
}
