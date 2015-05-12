// MODULE: m1
// FILE: x.kt
package p

public interface Base {
    public fun <T> foo(t: Array<T>) {}
}

public interface A : Base

// MODULE: m2
// FILE: x.kt
package p

public interface Base {
    public fun <T: Base> foo(t: Array<T>) {}
}

public interface B : Base

// MODULE: m3(m1, m2)
// FILE: x.kt

import p.*

class Foo: A, B {
    override fun <T> foo(t: Array<T>) {}
    override fun <T: Base> foo(t: Array<T>) {}
}