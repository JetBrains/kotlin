// FIR_IDENTICAL
class A<T : A<T>>
fun <T : A<*>> foo() {}
class B<T : A<*>>