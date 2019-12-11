open class A {
    private fun foo() {}

    inner class B : A() {
        private fun foo() {}
    }
}

class C : A() {
    private fun foo() {}
}