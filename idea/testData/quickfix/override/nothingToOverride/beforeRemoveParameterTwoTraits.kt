// "Change function signature to 'override fun f()'" "true"
trait A {
    fun f()
}
trait B {
    fun f()
}

class C : A, B {
    <caret>override fun f(a: String) {}
}
