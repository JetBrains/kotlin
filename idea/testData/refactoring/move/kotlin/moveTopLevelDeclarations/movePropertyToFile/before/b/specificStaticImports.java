package b;

import static a.APackage.getTest;
import static a.APackage.setTest;

class J {
    void bar() {
        setTest("");
        System.out.println(getTest());
    }
}
