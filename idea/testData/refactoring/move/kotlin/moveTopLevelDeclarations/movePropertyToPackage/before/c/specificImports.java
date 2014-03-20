package c;

import a.APackage;

class J {
    void bar() {
        APackage.setTest("");
        System.out.println(APackage.getTest());
    }
}
