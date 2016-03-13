// FILE: test/En.java

package test;

public enum En {
    A;
}

// FILE: 1.kt

import test.En.A

fun box() =
    if (A.toString() == "A") "OK"
    else "fail"
