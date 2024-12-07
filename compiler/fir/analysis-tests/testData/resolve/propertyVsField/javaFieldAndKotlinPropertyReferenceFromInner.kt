// RUN_PIPELINE_TILL: BACKEND
// FILE: base/Jaba.java

package base;

public class Jaba {
    protected String a = "FAIL";
}

// FILE: test.kt

import base.Jaba

fun box(): String {
    val x = object : Jaba() {
        private val <!PROPERTY_HIDES_JAVA_FIELD!>a<!>: String = "OK"
        inner class S {
            // Should be resolved to a property
            fun foo() = a
        }

        fun bar() = S().foo()
    }

    return x.bar()
}
