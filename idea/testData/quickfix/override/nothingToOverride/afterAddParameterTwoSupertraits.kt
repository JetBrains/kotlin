// "Change function signature to 'override fun f(a: Int)'" "true"
trait A {
    fun f(a: Int)
}

trait B : A {
}

class C : B {
    override fun f(a: Int) {}
}
