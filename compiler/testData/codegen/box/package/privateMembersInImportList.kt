package test

import test.C.E1
import test.A.B.*
import test.Obj.CInObj.Tt
import test.Obj.foo

private enum class C {
    E1
}

class A {
    private class B {
        object C
        class D
    }

    fun test() {
        C
        D()
    }
}

private object Obj {
    private class CInObj {
        class Tt
    }

    fun foo() {
        Tt()
    }
}

fun box(): String {
    E1
    A().test()
    foo()

    return "OK"
}