// MODULE: m1
// FILE: x.kt
package p

public trait Base {
    public fun <T> foo(t: Array<T>) {}
}

public trait A : Base

// MODULE: m2
// FILE: x.kt
package p

public trait Base {
    public fun <T: Base> foo(t: Array<T>) {}
}

public trait B : Base

// MODULE: m3(m1, m2)
// FILE: x.kt

import p.*

class Foo: A, B {
    override fun <T> foo(t: Array<T>) {}
    override fun <T: Base> foo(t: Array<T>) {}
}