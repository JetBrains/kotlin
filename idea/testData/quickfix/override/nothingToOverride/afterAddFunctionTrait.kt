// "Add 'open fun f()' to 'A'" "true"
trait A {
    open fun f()
}
class B : A {
    <caret>override fun f() {}
}