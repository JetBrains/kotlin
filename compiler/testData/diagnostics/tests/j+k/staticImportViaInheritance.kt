// FIR_IDENTICAL
// ISSUE: KT-59140

// FILE: pkg/Foo.java

package pkg;

abstract class CommonFoo {
    public static final int BAR = 1;
}

public class Foo extends CommonFoo {}

// FILE: test.kt

import pkg.Foo
import pkg.Foo.BAR

fun test() {
    val bar = BAR
    val fooBar = Foo.BAR
}
