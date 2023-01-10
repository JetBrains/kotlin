// FILE: A.java

package base;

public class A {
    String f = "OK";
}

// FILE: B.kt

package base

open class B : A() {
    private val f = "FAIL"
}

// FILE: C.java

import base.B;

public class C extends B {}

// FILE: test.kt

fun box(): String {
    return C().<!INVISIBLE_MEMBER!>f<!>
}
