val (@[a] T<T>.(A<B>) -> Unit).foo: P
val (@[a] T<T>.(A<B>) ->  C<D, E>).foo: P
val @[a] (@[a] T<T>.(A<B>) -> R).foo: P
val <A, B> @[a] (() -> Unit).foo: P
@[a] val <A, B> @[a] ((A, B) -> Unit).foo: P
