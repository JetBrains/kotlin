// "Change 'A.foo' function return type to 'Long'" "true"
trait A {
    fun foo(): Long
}

trait B {
    fun foo(): Number
}

trait C : A, B {
    override fun foo(): Long<caret>
}