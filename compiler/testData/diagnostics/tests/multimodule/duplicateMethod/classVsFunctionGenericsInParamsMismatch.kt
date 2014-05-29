// MODULE: m1
// FILE: a.kt
package p

public trait B<T> {
    public fun <R> foo(a: T, b : R)
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public trait C : B<Any?> {
    override fun <R> foo(a: Any?, b : R)

}

// MODULE: m3
// FILE: b.kt
package p

public trait B {
    public fun <T, R> foo(a: T, b: R)
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B?, c: C) {
    b?.foo(1, 1)
    c.foo(1, 1)
    if (b is C) {
        b?.<!CANNOT_COMPLETE_RESOLVE!>foo<!>(1, 1)
    }
}