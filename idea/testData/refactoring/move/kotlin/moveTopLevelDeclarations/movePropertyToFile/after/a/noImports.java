package a;

import b.BPackage;

class J {
    void bar() {
        BPackage.setTest("");
        System.out.println(BPackage.getTest());
    }
}
