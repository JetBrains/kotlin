// "Add 'open fun f()' to 'A'" "true"
trait A
class B : A {
    <caret>override fun f() {}
}