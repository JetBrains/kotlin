package b;

import static b.BPackage.getTest;
import static b.BPackage.setTest;

class J {
    void bar() {
        setTest("");
        System.out.println(getTest());
    }
}
