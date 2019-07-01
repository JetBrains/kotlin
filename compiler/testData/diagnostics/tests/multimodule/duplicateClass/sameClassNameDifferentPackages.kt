// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE
// MODULE: m1
// FILE: a.kt

package p

public class A
public class B {
    public val a: A = A()
}

// MODULE: m2(m1)
// FILE: b.kt

import p.*

class A

fun test() {
    val a: A = <!TYPE_MISMATCH!>B().a<!>
}