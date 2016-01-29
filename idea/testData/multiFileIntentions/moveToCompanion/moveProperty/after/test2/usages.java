package test2;

import test.A;

class Test {
    void foo() {
        int x = A.Companion.getTest();
        A.Companion.setTest(1);
    }
}