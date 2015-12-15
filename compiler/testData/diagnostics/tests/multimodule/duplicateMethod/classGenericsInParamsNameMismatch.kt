// !DIAGNOSTICS: -UNNECESSARY_SAFE_CALL

// MODULE: m1
// FILE: a.kt
package p

public interface B<T> {
    public fun foo(a: T)
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public interface C<X> : B<X> {
    override fun foo(a: X)

}

// MODULE: m3
// FILE: b.kt
package p

public interface B<T1> {
    public fun foo(a: T1)
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun <Y> test(b: B<Y>?, y: Y) {
    if (b is C) {
        b?.foo(y)
    }
}
