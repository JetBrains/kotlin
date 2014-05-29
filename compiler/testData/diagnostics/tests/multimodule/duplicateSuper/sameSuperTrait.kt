// MODULE: m1
// FILE: x.kt
package p

public trait Base {
    public fun foo() {}
}

public trait A : Base

// MODULE: m2
// FILE: x.kt
package p

public trait Base {
    public fun foo() {}
}

public trait B : Base

// MODULE: m3(m1, m2)
// FILE: x.kt

import p.*

class Foo: A, B