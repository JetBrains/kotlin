// !DIAGNOSTICS: -UNNECESSARY_SAFE_CALL

// MODULE: m0
// FILE: a.kt
package p

public interface G1<T>
public interface G2<A, B>

// MODULE: m1(m0)
// FILE: a.kt
package p

public interface B {
    public fun foo(a: G1<Int>, b: G2<B, String>): B?
}

// MODULE: m2(m1, m0)
// FILE: b.kt
package p

public interface C : B {
    override fun foo(a: G1<Int>, b: G2<B, String>): B?

}

// MODULE: m3(m0)
// FILE: b.kt
package p

public interface B {
    public fun foo(a: G1<Int>, b: G2<B, String>): B?
}

// MODULE: m4(m3, m2, m0)
// FILE: c.kt
import p.*

fun test(b: B?, a: G1<Int>, b1: G2<B, String>) {
    if (b is C) {
        b?.foo(a, b1)
    }
}
