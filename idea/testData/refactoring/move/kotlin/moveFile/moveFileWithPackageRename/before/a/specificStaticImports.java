package a;

import static a.MainKt.test;
import static a.MainKt.getTEST;
import static a.MainKt.setTEST;

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
