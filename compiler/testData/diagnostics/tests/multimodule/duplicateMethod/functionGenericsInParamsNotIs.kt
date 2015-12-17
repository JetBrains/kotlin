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

public interface B {
    public fun <T> foo(a: T): B?
}

// MODULE: m4(m3, m2)
// FILE: c.kt
import p.*

fun test(b: B?) {
    if (b !is C)  return
    b?.foo("")
}

fun test1(b: B?) {
    if (b !is C)  return
    b?.foo<String>("")
}
