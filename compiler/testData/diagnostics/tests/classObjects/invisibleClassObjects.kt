//FILE:a.kt
package a

class A {
    class object {
        fun foo() {}
    }
}

private class B {
    class object {
        fun bar() {}
    }
}

class C {
    private class object {
        fun baz() {}
    }
}

private class D {
    private class object {
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
    <!INVISIBLE_MEMBER!>B<!>.<!INVISIBLE_MEMBER!>bar<!>()
    <!INVISIBLE_MEMBER!>C<!>.<!INVISIBLE_MEMBER!>baz<!>()
    <!INVISIBLE_MEMBER!>D<!>.<!INVISIBLE_MEMBER!>quux<!>()

    a.A.foo()
    a.<!INVISIBLE_MEMBER!>C<!>.<!INVISIBLE_MEMBER!>baz<!>()
}

fun f(<!UNUSED_PARAMETER!>unused<!>: Any) {}
