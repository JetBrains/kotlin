// FILE: 1.kt
package test

object A { inline fun f() {} }
object B { inline fun g() {} }
object C { inline fun h() {} }

object D {
    inline fun together() {
        A.f()
        C.h()
        B.g()
    }
}

// FILE: 2.kt
import test.*

object X {
    // Unlike `rangeFolding.kt`, the calls in `D.together` refer to different
    // classes which are reflected in the SMAP, so they cannot be joined into
    // a single range even in `X.foo`; neither can lines corresponding to
    // `D.together` because they do not form an uninterrupted range.
    fun foo() = D.together()
}

fun box(): String {
    X.foo()
    return "OK"
}
