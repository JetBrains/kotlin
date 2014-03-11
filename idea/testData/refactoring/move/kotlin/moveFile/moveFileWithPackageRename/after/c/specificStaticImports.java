package c;

import b.Test;

import static b.BPackage.test;
import static b.BPackage.getTEST;
import static b.BPackage.setTEST;

class J {
    void bar() {
        Test t = new Test();
        test();
        System.out.println(getTEST());
        setTEST("");
    }
}
