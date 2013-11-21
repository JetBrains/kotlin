class A {
    fun f(param: Int)
}

trait B {
    fun f(p: Int)
}

class C : A, B {
    override fun <caret>f(p: Int) {}
}