// "Change function signature to 'fun f()'" "true"
interface A {
    fun f()
}
interface B {
    fun f()
}

class C : A, B {
    <caret>override fun f(a: String) {}
}
