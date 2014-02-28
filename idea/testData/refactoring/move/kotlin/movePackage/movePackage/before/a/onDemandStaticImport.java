package a;

import static APackage.*;

class J {
    void bar() {
        A t = new A();
        foo();
        System.out.println(getX());
        setX("");
    }
}
