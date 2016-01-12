package test2;

import test.A;
import test.C;

class Test {
    C foo() {
        return new C(new A().new B());
    }
}