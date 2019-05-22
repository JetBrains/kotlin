// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: test/jv/JavaSample.java

package test.jv;

public class JavaSample {
    public static void member() {}
}

// FILE: foo.kt

package test.kot

typealias JavaAlias = test.jv.JavaSample

// FILE: test.kt

import test.kot.JavaAlias
import test.kot.JavaAlias.member

fun foo(
    sample: <!UNRESOLVED_REFERENCE!>JavaSample<!>,
    alias: JavaAlias
) {
    member()
}