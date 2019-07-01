// SKIP_IN_FIR_TEST
package test;

public class SubclassFromNested implements B.C {
}

class B {
    B(C c) {}

    interface C {
    }
}
