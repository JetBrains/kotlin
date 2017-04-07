package test.pack

import test.Bar

class A {
    val b = B()
}

class B {
    val a = A()
}

class C {
    internal val foo = Foo()
    internal val bar = Bar()
}