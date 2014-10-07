// !DIAGNOSTICS: -UNNECESSARY_SAFE_CALL

// MODULE: m1
// FILE: a.kt
package p

public trait B {
    public fun getParent(): B?
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public class C : B {
    override fun getParent(): B? = null

}

// MODULE: m3
// FILE: b.kt
package p

public trait B {
    public fun getParent(): B?
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B?) {
    if (b is C) {
        <!DEBUG_INFO_SMARTCAST!>b<!>?.getParent()
    }
}