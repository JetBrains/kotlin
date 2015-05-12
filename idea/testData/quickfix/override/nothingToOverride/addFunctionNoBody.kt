// "Add 'open fun f()' to 'A'" "true"
interface A
class B : A {
    <caret>override fun f() {}
}