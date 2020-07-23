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
    f(<!HIDDEN!>B<!>)
    f(C)
    f(<!HIDDEN!>D<!>)

    A.foo()
    <!HIDDEN!>B<!>.bar()
    C.baz()
    <!HIDDEN!>D<!>.quux()

    a.A.foo()
    a.C.baz()
}

fun f(unused: Any) {}
