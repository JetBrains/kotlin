package test2;

import test.A;
import test.A.Companion.B;

class Test {
    B foo() {
        return new B(new A());
    }
}