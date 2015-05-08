// "Change function signature to 'fun f()'" "true"
trait A {
    fun f()
}

class B : A {
    <caret>override fun f(a: Int) {}
}
