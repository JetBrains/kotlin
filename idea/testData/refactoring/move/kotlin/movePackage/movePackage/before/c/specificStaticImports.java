package c;

import static a.APackage.foo;
import static a.APackage.getX;
import static a.APackage.setX;

class J {
    void bar() {
        a.A t = new a.A();
        foo();
        System.out.println(getX());
        setX("");
    }
}
