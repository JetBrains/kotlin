class C<T : C<T>> {
    fun foo(c: C<*>) {}
}

open class Super<T>

class Sub: Super<C<*>>()

// class: Sub
// jvm signature:     Sub
// generic signature: LSuper<LC<*>;>;
