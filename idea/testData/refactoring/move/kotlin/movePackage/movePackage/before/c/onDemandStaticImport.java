package c;

import static a.APackage.*;

class J {
    void bar() {
        a.A t = new a.A();
        foo();
        System.out.println(getX());
        setX("");
    }
}
