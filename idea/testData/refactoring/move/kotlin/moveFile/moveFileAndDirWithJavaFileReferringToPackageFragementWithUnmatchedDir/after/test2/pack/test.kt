package test2.pack

import test.Bar
import test.pack.Foo

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