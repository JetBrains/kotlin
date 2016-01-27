package test2;

import test.A;
import test.A.B;

class Test {
    B foo() {
        return new A().new B();
    }
}