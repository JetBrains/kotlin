// RUN_PIPELINE_TILL: BACKEND
//ERROR_CALL 'Unresolved reference: <Unresolved name: OK>#' type=IrErrorType([Error type: Unresolved type for OK])
// MODULE: lib

// FILE: test/I.java
package test;

interface I {
    // Not a String to avoid constant inlining
    public static String[] OK = new String[]{"OK"};
}

// FILE: test/J.java
package test;

public class J implements I {}

// MODULE: main(lib)
// FILE: k.kt
import test.J

fun test() {
    <!DEBUG_INFO_CALLABLE_OWNER("test.I.OK in test.I")!>J.OK<!>
}
