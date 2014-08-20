package c;

import static a.APackage.getTest;
import static a.APackage.setTest;

class J {
    void bar() {
        getTest(new a.Test());
        setTest(new a.Test(), 0);
    }
}
