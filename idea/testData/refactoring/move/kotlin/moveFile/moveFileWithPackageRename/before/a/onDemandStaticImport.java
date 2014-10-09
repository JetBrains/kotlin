package a;

import static a.APackage.*;

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
