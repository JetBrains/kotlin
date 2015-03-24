package a;

import b.BPackage;

class J {
    void bar() {
        BPackage.getTest(new Test());
        BPackage.setTest(new Test(), 0);
    }
}
