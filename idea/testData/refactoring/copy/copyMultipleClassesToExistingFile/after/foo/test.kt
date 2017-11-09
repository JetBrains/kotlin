package foo

class A {
    val a: A = A()
    val b: B = B()
    val c: C = C()
}

class B {
    val a: A = A()
    val b: B = B()
    val c: C = C()
}

class C {
    val a: A = A()
    val b: B = B()
    val c: C = C()
}