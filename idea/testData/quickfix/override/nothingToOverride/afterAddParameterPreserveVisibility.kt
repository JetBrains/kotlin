// "Change function signature to 'public override fun f(a: Int)'" "true"
trait A {
    fun f(a: Int)
}

class B : A {
    public <caret>override fun f(a: Int) {}
}
