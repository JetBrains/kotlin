// !DIAGNOSTICS: -UNNECESSARY_SAFE_CALL

// MODULE: m1
// FILE: a.kt
package p

public interface B {
    public fun <T> foo(a: T): B?
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public interface C : B {
    override fun <T> foo(a: T): B?

}

// MODULE: m3
// FILE: b.kt
package p

public interface Tr

public interface B {
    public fun <T: Tr?> foo(a: T): B?
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B?) {
    if (b is C) {
        // hard to find parameters for an ambiguous call, so we rely on NONE_APPLICABLE here
        // as opposed to diagnostics for a single unmatched candidate
        b?.<!NONE_APPLICABLE!>foo<!>()
    }
}