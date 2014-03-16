package a;

import static a.APackage.test;
import static a.APackage.getTEST;
import static a.APackage.setTEST;

class J {
    void bar() {
        Test t = new Test();
        test();
        System.out.println(getTEST());
        setTEST("");
    }
}
