class C<T : C<T>>

fun bar(c: C<*>): C<*> = null!!

// method: _DefaultPackage::bar
// jvm signature:     (LC;)LC;
// generic signature: (LC<*>;)LC<*>;