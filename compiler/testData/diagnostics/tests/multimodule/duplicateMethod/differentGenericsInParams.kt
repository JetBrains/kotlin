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
    public fun foo(a: G1<Int>?, b: G2<B, String>?)
}

// MODULE: m2(m1, m0)
// FILE: b.kt
package p

public interface C : B {
    override fun foo(a: G1<Int>?, b: G2<B, String>?)

}

// MODULE: m3(m0)
// FILE: b.kt
package p

public interface B {
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