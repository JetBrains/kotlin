package c;

import a.Test;
import b.BPackage;

class J {
    void bar() {
        BPackage.getTest(new Test());
        BPackage.setTest(new Test(), 0);
    }
}
