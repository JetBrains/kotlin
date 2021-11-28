// FILE: 1.kt

package test

open class A {
    open val test = "OK"
}

object X : A() {
    override val test: String
        get() = "fail"

    <!NOTHING_TO_INLINE!>inline<!> fun doTest(): String {
        <!SUPER_CALL_FROM_PUBLIC_INLINE_ERROR!>return super.test<!>
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    return X.doTest()
}
