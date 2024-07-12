// SKIP_IN_FIR_TEST
// Reason: KT-4455
package test;

public class SubclassFromNested implements B.C {
}

class B {
    B(C c) {}

    interface C {
    }
}
