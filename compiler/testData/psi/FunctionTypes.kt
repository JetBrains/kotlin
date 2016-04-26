typealias f =  (@[a] a) -> b
typealias f =  (a) -> b
typealias f =  () -> @[x] b
typealias f =  () -> Unit

typealias f =  (a : @[a] a) -> b
typealias f =  (a : a) -> b
typealias f =  () -> b
typealias f =  () -> Unit

typealias f =  (a : @[a] a, foo, x : bar) -> b
typealias f =  (foo, a : a) -> b
typealias f =  (foo, a :  (a) -> b) -> b
typealias f =  (foo, a :  (a) -> b) ->  () -> Unit

typealias f =  T.() -> Unit
typealias f =  T.T.() -> Unit
typealias f =  T<A, B>.T<x>.() -> Unit

typealias f = @[a]  T.() -> Unit
typealias f = @[a]  T.T.() -> Unit
typealias f = @[a]  T<A, B>.T<x>.() -> Unit
