fun (@[a] T<T>.(A<B>) -> Unit).foo()
fun (@[a] T<T>.(A<B>) ->  C<D, E>).foo();
fun @[a] (@[a] T<T>.(A<B>) -> R).foo() {}
fun <A, B> @[a] (() -> Unit).foo()
@[a] fun <A, B> @[a] ((A, B) -> Unit).foo()
