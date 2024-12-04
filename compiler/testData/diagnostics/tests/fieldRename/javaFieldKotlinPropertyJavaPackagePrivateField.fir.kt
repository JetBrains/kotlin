// RUN_PIPELINE_TILL: FRONTEND
// FILE: base/A.java

package base;

public class A {
    String f = "OK";
}

// FILE: B.kt

package base

open class B : A() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>f<!> = "FAIL"
}

// FILE: C.java

import base.B;

public class C extends B {}

// FILE: test.kt

fun box(): String {
    return C().<!INVISIBLE_REFERENCE!>f<!>
}
