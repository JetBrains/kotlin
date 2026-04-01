// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: test/En.java

package test;

public enum En {
    A;
}

// MODULE: main(lib)
// FILE: 1.kt

import test.En.A

fun box() =
    if (A.toString() == "A") "OK"
    else "fail"
