// MODULE: m1
// FILE: x.kt
package p

public trait Base<T> {
    public fun foo(t: T) {}
}

public trait A<T> : Base<T>

// MODULE: m2
// FILE: x.kt
package p

public trait Base<T> {
    public fun foo(t: T) {}
}

public trait B : Base<String>

// MODULE: m3(m1, m2)
// FILE: x.kt

import p.*

class Foo: A<String>, B