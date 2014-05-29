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
    public fun foo(a: G1<Int>?, b: G2<B, String>?)
}

// MODULE: m2(m1, m0)
// FILE: b.kt
package p

public trait C : B {
    override fun foo(a: G1<Int>?, b: G2<B, String>?)

}

// MODULE: m3(m0)
// FILE: b.kt
package p

public trait B {
    public fun foo(a: G1<out Any?>?, b: G2<Int, out Any?>?)
}

// MODULE: m4(m3, m2, m0)
// FILE: c.kt
import p.*

fun test(b: B?) {
    if (b is C) {
        b?.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(null, null)
    }
}