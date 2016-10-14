package p

import p.Outer.Inner1
import p.Outer.Inner2

class Outer {
    inner class Inner1
    inner class Inner2
}

fun foo(p: Outer.() -> Unit){}

fun bar() {
    foo {
        fun Inner1.f() {}
        Inner2()
    }
}
