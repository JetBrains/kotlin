class C<T : C<T>> {
    fun foo(c: C<*>): C<*> = null!!
}

// method: C::foo
// jvm signature:     (LC;)LC;
// generic signature: (LC<*>;)LC<*>;