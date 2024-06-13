// FIR_IDENTICAL
// ISSUE: KT-59140

// FILE: pkg/CommonFoo.java

package pkg;

abstract class CommonFoo {
    public static final int BAR = 1;
}

// FILE: pkg/Foo.java

package pkg;

public class Foo extends CommonFoo {}

// FILE: test.kt

import pkg.Foo
import pkg.Foo.BAR

fun test() {
    val bar = BAR
    val fooBar = Foo.BAR
}
