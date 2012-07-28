open class A {
    private fun foo() : Int = 1
}

class B : A() {
    fun foo() : String = ""
}