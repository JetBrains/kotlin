// FIR_IDENTICAL
// MODULE: m1
// FILE: a.kt
package p

public interface B<T> {
    public fun foo(a: T)
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public interface C : B<String> {
    override fun foo(a: String)

}

// MODULE: m3
// FILE: b.kt
package p

public interface B {
    public fun foo(a: String)
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B?) {
    if (b is C) {
        b<!UNNECESSARY_SAFE_CALL!>?.<!>foo("")
    }
}
