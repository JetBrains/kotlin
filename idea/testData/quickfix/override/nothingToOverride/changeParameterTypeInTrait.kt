// "Change function signature to 'fun f(a: Int)'" "true"
trait A {
    fun f(a: Int)
}

trait B : A {
    <caret>override fun f(a: String)
}
