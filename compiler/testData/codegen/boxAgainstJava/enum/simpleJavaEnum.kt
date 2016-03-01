// FILE: test/En.java

package test;

public enum En {
    A;
}

// FILE: 1.kt

import test.*

fun box() =
    if (En.A.toString() == "A") "OK"
    else "fail"
