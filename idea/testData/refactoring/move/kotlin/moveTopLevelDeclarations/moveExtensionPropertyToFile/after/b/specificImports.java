package b;

import a.Test;

class J {
    void bar() {
        b.BPackage.getTest(new Test());
        b.BPackage.setTest(new Test(), 0);
    }
}
