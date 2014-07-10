package b;

import a.*;

class J {
    void bar() {
        APackage.getTest(new Test());
        APackage.setTest(new Test(), 0);
    }
}
