package c;

import static a.APackage.test;
import static a.APackage.getTEST;
import static a.APackage.setTEST;

class J {
    void bar() {
        a.Test t = new a.Test();
        test();
        test(t);
        System.out.println(getTEST());
        System.out.println(getTEST(t));
        setTEST("");
        setTEST(t, "");
    }
}
