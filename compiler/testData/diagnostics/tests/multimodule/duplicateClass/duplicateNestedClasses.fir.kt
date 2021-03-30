// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// MODULE: m1
// FILE: a.kt

package p

public class A {
    public class B
    public object C
    companion object {
        public class D {
            public object E
        }
        public class G
    }

    public inner class F
}

public class M1 {
    public val a: A = A()
    public val b: A.B = A.B()
    public val c: A.C = A.C
    public val d: A.Companion.D = A.Companion.D()
    public val e: A.Companion.D.E = A.Companion.D.E
    public val f: A.F = A().F()
    public val g: A.Companion.G = A.Companion.G()
}

// MODULE: m2
// FILE: b.kt

package p

public class A {
    public class B
    public class C
    companion object {
        public class D {
            public class E
        }
    }
    public class G
    public inner class F
}

public fun a(p: A) {}
public fun b(p: A.B) {}
public fun c(p: A.C) {}
public fun d(p: A.Companion.D) {}
public fun e(p: A.Companion.D.E) {}
public fun f(p: A.F) {}
public fun g(p: A.G) {}

// MODULE: m3(m1, m2)
// FILE: b.kt

import p.*

fun test(m1: M1) {
    a(m1.a)
    b(m1.b)
    c(m1.c)
    d(m1.d)
    e(m1.e)
    f(m1.f)
    g(<!ARGUMENT_TYPE_MISMATCH!>m1.g<!>)
}