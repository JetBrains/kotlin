// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// MODULE: m1
// FILE: a.kt

package p

public interface A<T>
public interface C
public interface D<T>
public class B : A<Int>, C, D<Int>
public class M1 {
    public val b: B = B()
}

// MODULE: m2
// FILE: b.kt

package p

public interface A
public interface C<T>
public interface D<T>

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