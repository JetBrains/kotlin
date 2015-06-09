val ((T) -> G).foo<P> { }
val ((T) -> G).foo get{ }
val ((T) -> G).foo<P>
val ((T) -> G).foo: = 0
val ((T) -> G)?.foo
val ((T) -> G)??.foo

val (T<T>.(A<B>, C<D, E>) -> ).foo {}
val val @a T<T>.(A<B>).foo()

val @[a] (T<T>.(A<B>)).foo()
val @[a] ((A<B>)-).foo()

val c<T> by A.B