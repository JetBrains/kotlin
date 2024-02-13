// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_JAVAC

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
import test.kot.<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_WARNING!>JavaAlias<!>.member

fun foo(
    sample: <!UNRESOLVED_REFERENCE!>JavaSample<!>,
    alias: JavaAlias
) {
    member()
}
