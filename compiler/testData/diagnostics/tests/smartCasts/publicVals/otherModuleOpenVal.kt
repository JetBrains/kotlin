// SKIP_TXT
// FIR_IDENTICAL
// MODULE: m1
// FILE: a.kt

package a

open class C {
    open val x: String? = null
}

// MODULE: m2(m1)
// FILE: b.kt

package b

import a.C

class D : C()

fun D.test(): Int {
    x!!
    // Although D is final and the getter is not overridden, C is in another module.
    return <!SMARTCAST_IMPOSSIBLE!>x<!>.length
}
