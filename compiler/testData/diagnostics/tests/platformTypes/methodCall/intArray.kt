// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: p/J.java

package p;

public class J {
    public void intArr(int[] s) {}
}

// FILE: k.kt

import p.*

fun test(ia: IntArray) {
    J().intArr(ia)
    J().intArr(null)
}