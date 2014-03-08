package c;

import static b.a.APackage.foo;
import static b.a.APackage.getX;
import static b.a.APackage.setX;

class J {
    void bar() {
        b.a.A t = new b.a.A();
        foo();
        System.out.println(getX());
        setX("");
    }
}
