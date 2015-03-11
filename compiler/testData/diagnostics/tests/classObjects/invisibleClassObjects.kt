//FILE:a.kt
package a

class A {
    default object {
        fun foo() {}
    }
}

private class B {
    default object {
        fun bar() {}
    }
}

class C {
    private default object {
        fun baz() {}
    }
}

private class D {
    private default object {
        fun quux() {}
    }
}

//FILE:b.kt
package b

import a.A
import a.<!INVISIBLE_REFERENCE!>B<!>
import a.C
import a.<!INVISIBLE_REFERENCE!>D<!>

fun test() {
    f(A)
    f(<!INVISIBLE_MEMBER!>B<!>)
    f(<!INVISIBLE_MEMBER!>C<!>)
    f(<!INVISIBLE_MEMBER!>D<!>)

    A.foo()
    B.<!INVISIBLE_MEMBER!>bar<!>()
    C.<!INVISIBLE_MEMBER!>baz<!>()
    D.<!INVISIBLE_MEMBER!>quux<!>()

    a.A.foo()
    a.C.<!INVISIBLE_MEMBER!>baz<!>()
}

fun f(<!UNUSED_PARAMETER!>unused<!>: Any) {}
