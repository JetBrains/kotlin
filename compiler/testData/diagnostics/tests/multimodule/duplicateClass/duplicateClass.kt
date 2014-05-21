// !DIAGNOSTICS: -UNUSED_VARIABLE
// MODULE: m1
// FILE: a.kt

package p

public class A
public class B {
    public val a: A = A()
}

// MODULE: m2
// FILE: b.kt

package p

public class A {
    val x = 1
}

public fun foo(a: A) {
    a.x + 1
}

// MODULE: m3(m1, m2)
// FILE: b.kt

import p.*

fun test() {
    foo(B().a)
}