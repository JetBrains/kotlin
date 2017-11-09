package bar

import foo.B

class A {
    val a: A = A()
    val b: B = B()
    val c: C = C()
}

class C {
    val a: A = A()
    val b: B = B()
    val c: C = C()
}