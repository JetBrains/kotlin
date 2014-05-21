// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// MODULE: m1
// FILE: a.kt

package p

public class A
public class M1 {
    public val a: A = A()
}

// MODULE: m2
// FILE: b.kt

package p

public class A<T>

public fun foo(a: A<Int>) {
}

// MODULE: m3(m1, m2)
// FILE: b.kt

import p.*

fun test() {
    foo(<!TYPE_MISMATCH!>M1().a<!>)
}