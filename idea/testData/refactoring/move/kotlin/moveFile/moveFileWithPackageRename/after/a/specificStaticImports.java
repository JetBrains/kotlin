package a;

import b.Test;

import static b.BPackage.test;
import static b.BPackage.getTEST;
import static b.BPackage.setTEST;

class J {
    void bar() {
        Test t = new Test();
        test();
        test(t);
        System.out.println(getTEST());
        System.out.println(getTEST(t));
        setTEST("");
        setTEST(t, "");
    }
}
