package test2;

import test.A;

class Test {
    A.Companion.B foo() {
        return new A.Companion.B(new A());
    }
}