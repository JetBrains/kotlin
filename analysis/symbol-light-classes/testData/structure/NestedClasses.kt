open class A {
    class NestedInA {
        open class NestedInNestedInA

        inner class InnerInNestedInA
    }

    inner class InnerInA {
        inner class InnerInInnerInA
    }
}

class B : A() {
    class NestedInB : NestedInA.NestedInNestedInA()

    inner class InnerInB
}
