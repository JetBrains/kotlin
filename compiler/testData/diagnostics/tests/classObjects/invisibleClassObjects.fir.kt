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
    f(<!INVISIBLE_REFERENCE!>B<!>)
    f(C)
    f(<!INVISIBLE_REFERENCE!>D<!>)

    A.foo()
    <!INVISIBLE_REFERENCE!>B<!>.<!INVISIBLE_REFERENCE!>bar<!>()
    C.<!INVISIBLE_REFERENCE!>baz<!>()
    <!INVISIBLE_REFERENCE!>D<!>.<!INVISIBLE_REFERENCE!>quux<!>()

    a.A.foo()
    a.C.<!INVISIBLE_REFERENCE!>baz<!>()
}

fun f(unused: Any) {}
