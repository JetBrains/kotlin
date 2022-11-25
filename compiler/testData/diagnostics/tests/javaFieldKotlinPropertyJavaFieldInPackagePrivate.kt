// FILE: A.java

package base;

class A {
    public String f = "OK";
}

// FILE: B.kt

package base

open class B : <!EXPOSED_SUPER_CLASS!>A()<!> {
    private val f = "FAIL"
}

// FILE: C.java

import base.B;

public class C extends B {}

// FILE: test.kt

fun box(): String {
    return C().f
}
