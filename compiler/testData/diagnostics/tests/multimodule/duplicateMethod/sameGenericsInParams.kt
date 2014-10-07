// !DIAGNOSTICS: -UNNECESSARY_SAFE_CALL

// MODULE: m0
// FILE: a.kt
package p

public trait G1<T>
public trait G2<A, B>

// MODULE: m1(m0)
// FILE: a.kt
package p

public trait B {
    public fun foo(a: G1<Int>, b: G2<B, String>): B?
}

// MODULE: m2(m1, m0)
// FILE: b.kt
package p

public trait C : B {
    override fun foo(a: G1<Int>, b: G2<B, String>): B?

}

// MODULE: m3(m0)
// FILE: b.kt
package p

public trait B {
    public fun foo(a: G1<Int>, b: G2<B, String>): B?
}

// MODULE: m4(m3, m2, m0)
// FILE: c.kt
import p.*

fun test(b: B?, a: G1<Int>, b1: G2<B, String>) {
    if (b is C) {
        <!DEBUG_INFO_SMARTCAST!>b<!>?.foo(a, b1)
    }
}