// FILE: a/M.java
package a;

public class M {
    int m;
}

// FILE: b/F.java
package b;

import a.M;

public class F extends M {
}

// FILE: c.kt
package c

import b.F

fun f() {
    F().<!INVISIBLE_REFERENCE!>m<!>
}
