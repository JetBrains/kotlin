// "Change method signature to 'override fun f(a: Int)'" "true"
trait A {
    fun f(a: Int);
}

trait B : A {
    <caret>override fun f(a: Int);
}
