package c;

import b.Test;

class J {
    void bar() {
        Test t = new Test();
        b.BPackage.test();
        b.BPackage.test(t);
        System.out.println(b.BPackage.getTEST());
        System.out.println(b.BPackage.getTEST(t));
        b.BPackage.setTEST("");
        b.BPackage.setTEST(t, "");
    }
}
