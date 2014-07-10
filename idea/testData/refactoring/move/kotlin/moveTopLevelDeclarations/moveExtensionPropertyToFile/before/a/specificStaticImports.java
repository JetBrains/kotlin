package a;

import static a.APackage.getTest;
import static a.APackage.setTest;

class J {
    void bar() {
        getTest(new Test());
        setTest(new Test(), 0);
    }
}
