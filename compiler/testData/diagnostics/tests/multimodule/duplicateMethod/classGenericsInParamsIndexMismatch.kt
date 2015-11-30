// MODULE: m1
// FILE: a.kt
package p

public interface B<T, Z> {
    public fun foo(a: T?)
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public interface C<X, Z> : B<X, Z> {
    override fun foo(a: X?)

}

// MODULE: m3
// FILE: b.kt
package p

public interface B<Z, T> {
    public fun foo(a: T?)
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun <Y, Z> test(b: B<Y, Z>?) {
    if (b is C<Y, Z>) {
        b<!UNNECESSARY_SAFE_CALL!>?.<!><!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(null)
    }
}