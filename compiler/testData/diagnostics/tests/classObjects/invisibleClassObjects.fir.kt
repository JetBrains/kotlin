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
    f(<!INVISIBLE_REFERENCE!>B<!>)
    f(C)
    f(<!INVISIBLE_REFERENCE!>D<!>)

    A.foo()
    <!INVISIBLE_REFERENCE!>B<!>.bar()
    C.baz()
    <!INVISIBLE_REFERENCE!>D<!>.quux()

    a.A.foo()
    a.C.baz()
}

fun f(unused: Any) {}
