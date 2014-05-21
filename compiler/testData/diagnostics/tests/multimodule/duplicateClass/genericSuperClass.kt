// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// MODULE: m1
// FILE: a.kt

package p

public trait A<T>
public trait C
public trait D<T>
public class B : A<Int>, C, D<Int>
public class M1 {
    public val b: B = B()
}

// MODULE: m2
// FILE: b.kt

package p

public trait A
public trait C<T>
public trait D<T>

public fun a(a: A) {
}

public fun c(c: C<Int>) {
}

public fun d(d: D<Int>) {
}

// MODULE: m3(m1, m2)
// FILE: b.kt

import p.*

fun test() {
    a(<!TYPE_MISMATCH!>M1().b<!>) // Type arguments do not match
    c(<!TYPE_MISMATCH!>M1().b<!>) // Type arguments do not match
    d(M1().b) // Type arguments do match
}