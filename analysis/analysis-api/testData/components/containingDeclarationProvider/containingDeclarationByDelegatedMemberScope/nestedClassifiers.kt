interface A {
    class NestedInA

    fun foo()
}

class <caret>B(a: A) : A by a {
    class NestedInB

    inner class InnerInB
}
