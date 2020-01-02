// MODULE: m1
// FILE: x.kt
package p

public interface Base {
    public fun foo() {}
}

public interface A : Base {
    override fun foo() {}
}

public interface C : A


// MODULE: m2
// FILE: x.kt
package p

public interface Base {
    public fun foo() {}
}

public interface B : Base

// MODULE: m3(m1, m2)
// FILE: x.kt

import p.*

class Foo: C, B