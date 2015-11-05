//FILE:a.kt
package a

class A {
    companion object {
        fun foo() {}
    }
}

private class B {
    companion object {
        fun bar() {}
    }
}

class C {
    private companion object {
        fun baz() {}
    }
}

private class D {
    private companion object {
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
    <!INVISIBLE_REFERENCE!>B<!>.<!INVISIBLE_MEMBER!>bar<!>()
    C.<!INVISIBLE_MEMBER!>baz<!>()
    <!INVISIBLE_REFERENCE!>D<!>.<!INVISIBLE_MEMBER!>quux<!>()

    a.A.foo()
    a.C.<!INVISIBLE_MEMBER!>baz<!>()
}

fun f(<!UNUSED_PARAMETER!>unused<!>: Any) {}