package c;

import a.*;

class J {
    void bar() {
        A t = new A();
        APackage.foo();
        System.out.println(APackage.getX());
        APackage.setX("");
    }
}
