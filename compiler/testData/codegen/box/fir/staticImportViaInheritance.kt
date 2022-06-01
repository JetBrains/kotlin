// TARGET_BACKEND: JVM_IR
// ISSUE: KT-59140

// IGNORE_LIGHT_ANALYSIS
// ^ MISSING_DEPENDENCY_SUPERCLASS: Cannot access 'pkg.CommonFoo' which is a supertype of 'pkg.Foo'. Check your module classpath for missing or conflicting dependencies

// FILE: pkg/Foo.java

package pkg;

abstract class CommonFoo {
    public static final String BAR = "OK";
}

public class Foo extends CommonFoo {}

// FILE: test.kt

import pkg.Foo.BAR

fun box(): String {
    return BAR
}
