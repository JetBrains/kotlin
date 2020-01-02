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
import a.B
import a.C
import a.D

fun test() {
    f(A)
    f(<!INAPPLICABLE_CANDIDATE!>B<!>)
    f(C)
    f(<!INAPPLICABLE_CANDIDATE!>D<!>)

    A.foo()
    <!INAPPLICABLE_CANDIDATE!>B<!>.<!UNRESOLVED_REFERENCE!>bar<!>()
    C.baz()
    <!INAPPLICABLE_CANDIDATE!>D<!>.<!UNRESOLVED_REFERENCE!>quux<!>()

    a.A.foo()
    a.C.baz()
}

fun f(unused: Any) {}