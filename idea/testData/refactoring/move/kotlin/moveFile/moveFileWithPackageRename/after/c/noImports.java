package c;

import b.MainKt;
import b.Test;

class J {
    void bar() {
        Test t = new Test();
        MainKt.test();
        MainKt.test(t);
        System.out.println(MainKt.getTEST());
        System.out.println(MainKt.getTEST(t));
        MainKt.setTEST("");
        MainKt.setTEST(t, "");
    }
}
