// !DIAGNOSTICS: -BASE_WITH_NULLABLE_UPPER_BOUND -UNNECESSARY_SAFE_CALL

// MODULE: m1
// FILE: a.kt
package p

public trait B<T> {
    public fun foo(a: T?)
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public trait C<X> : B<X> {
    override fun foo(a: X?)

}

// MODULE: m3
// FILE: b.kt
package p

public trait Tr

public trait B<T: Tr?> {
    public fun foo(a: T?)
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B<Tr>?) {
    if (b is C) {
        <!DEBUG_INFO_SMARTCAST!>b<!>?.foo(null)
    }
}