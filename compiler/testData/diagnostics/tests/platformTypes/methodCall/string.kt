// FILE: p/J.java

package p;

public class J {
    public void str(J s) {}
}

// FILE: k.kt

import p.*

fun test() {
    J().str(J())
    J().str(null)
}