package test

abstract class A {
    class ClassInA

    inner class InnerClassInA
}

class B : A() {
    class ClassInB

    inner class InnerClassInB
}

// class: test/B
