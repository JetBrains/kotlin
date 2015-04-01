package a;

import b.BPackage;
import b.Test;

class J {
    void bar() {
        Test t = new Test();
        BPackage.test();
        BPackage.test(t);
        System.out.println(BPackage.getTEST());
        System.out.println(BPackage.getTEST(t));
        BPackage.setTEST("");
        BPackage.setTEST(t, "");
    }
}
