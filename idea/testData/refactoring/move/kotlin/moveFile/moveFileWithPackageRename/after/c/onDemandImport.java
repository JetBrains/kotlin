package c;

import b.Test;

class J {
    void bar() {
        Test t = new Test();
        b.BPackage.test();
        System.out.println(b.BPackage.getTEST());
        b.BPackage.setTEST("");
    }
}
