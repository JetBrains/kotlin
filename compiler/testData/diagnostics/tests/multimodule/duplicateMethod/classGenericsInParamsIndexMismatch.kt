// !DIAGNOSTICS: -BASE_WITH_NULLABLE_UPPER_BOUND

// MODULE: m1
// FILE: a.kt
package p

public trait B<T, _> {
    public fun foo(a: T?)
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public trait C<X, _> : B<X, _> {
    override fun foo(a: X?)

}

// MODULE: m3
// FILE: b.kt
package p

public trait B<_, T> {
    public fun foo(a: T?)
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun <Y, Z> test(b: B<Y, Z>?) {
    if (b is C<Y, Z>) {
        b?.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(null)
    }
}