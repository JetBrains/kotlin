package b;

import static a.APackage.*;

class J {
    void bar() {
        getTest(new a.Test());
        setTest(new a.Test(), 0);
    }
}
