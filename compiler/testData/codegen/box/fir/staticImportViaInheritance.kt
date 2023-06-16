// TARGET_BACKEND: JVM_IR
// ISSUE: KT-59140

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
