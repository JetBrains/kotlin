// "Change function signature to 'fun f(a: Int)'" "true"
trait A {
    fun f(a: Int)
}

trait B : A {
}

class C : B {
    <caret>override fun f() {}
}
