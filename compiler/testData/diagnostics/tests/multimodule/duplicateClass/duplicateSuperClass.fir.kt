// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// MODULE: m1
// FILE: a.kt

package p

public interface A
public class B : A
public class M1 {
    public val b: B = B()
}

// MODULE: m2
// FILE: b.kt

package p

public interface A

public fun foo(a: A) {
}

// MODULE: m3(m1, m2)
// FILE: b.kt

import p.*

fun test() {
    foo(M1().b)
}