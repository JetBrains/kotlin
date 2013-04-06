// "Change function signature to 'override fun f()'" "true"
trait A {
    fun f()
}

class B : A {
    <caret>override fun f() {}
}
