// RUN_PIPELINE_TILL: FRONTEND
// FILE: base/A.java

package base;

class A {
    public String f = "OK";
}

// FILE: B.kt

package base

open class B : <!EXPOSED_SUPER_CLASS!>A<!>() {
    private val <!PROPERTY_HIDES_JAVA_FIELD!>f<!> = "FAIL"
}

// FILE: C.java

import base.B;

public class C extends B {}

// FILE: test.kt

fun box(): String {
    return C().<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>f<!>
}
