// MODULE: m1
// FILE: a.kt
package p

public interface B {
    public fun getParent(): B?
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public interface C : B {
    override fun getParent(): B?

}

// MODULE: m3
// FILE: b.kt
package p

public interface B {
    public fun getParent(): B?
}

public interface D : B {
    override fun getParent(): B?
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B?) {
    if (b is C && b is D) {
        b<!UNNECESSARY_SAFE_CALL!>?.<!>getParent()
    }
}
