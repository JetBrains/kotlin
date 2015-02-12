class C<T : C<T>>

trait Base {
    fun foo(c: C<*>)
}

class Derived : Base {
    <caret>
}