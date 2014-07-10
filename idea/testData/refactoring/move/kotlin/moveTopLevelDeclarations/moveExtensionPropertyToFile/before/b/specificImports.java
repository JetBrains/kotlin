package b;

import a.APackage;
import a.Test;

class J {
    void bar() {
        APackage.getTest(new Test());
        APackage.setTest(new Test(), 0);
    }
}
