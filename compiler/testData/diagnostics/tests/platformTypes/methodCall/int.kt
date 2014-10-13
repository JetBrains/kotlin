// FILE: p/J.java

package p;

public class J {
    public void _int(int s) {}
}

// FILE: k.kt

import p.*

fun test() {
    J()._int(1)
    J()._int(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}