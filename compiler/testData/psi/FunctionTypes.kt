val v: (@[a] a) -> b
val v: (a) -> b
val v: () -> @[x] b
val v: () -> Unit

val v: (a : @[a] a) -> b
val v: (a : a) -> b
val v: () -> b
val v: () -> Unit

val v: (a : @[a] a, foo, x : bar) -> b
val v: (foo, a : a) -> b
val v: (foo, a :  (a) -> b) -> b
val v: (foo, a :  (a) -> b) ->  () -> Unit

//type f =  (ref foo, ref a :  (ref a) -> b) ->  () -> Unit

val v: T.() -> Unit
val v: T.T.() -> Unit
val v: T<A, B>.T<x>.() -> Unit

val v: @[a]  T.() -> Unit
val v: @[a]  T.T.() -> Unit
val v: @[a]  T<A, B>.T<x>.() -> Unit
