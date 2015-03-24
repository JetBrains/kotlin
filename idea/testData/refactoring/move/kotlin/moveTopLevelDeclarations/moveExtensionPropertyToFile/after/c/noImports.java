package c;

import b.BPackage;

class J {
    void bar() {
        BPackage.getTest(new a.Test());
        BPackage.setTest(new a.Test(), 0);
    }
}
