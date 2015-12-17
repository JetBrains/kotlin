// !DIAGNOSTICS: -UNNECESSARY_SAFE_CALL

// MODULE: m1
// FILE: a.kt
package p

public interface B {
    public fun String.getParent(): B?
}

// MODULE: m2(m1)
// FILE: b.kt
package p

public class C : B {
    override fun String.getParent(): B? = null

}

// MODULE: m3
// FILE: b.kt
package p

public interface B {
    public fun String.getParent(): B?
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun B.test() {
    if (this is C) {
        "".getParent()
    }
}
