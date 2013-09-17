type f =  ([a] a) -> b
type f =  (a) -> b
type f =  () -> [x] b
type f =  () -> Unit

type f =  (a : [a] a) -> b
type f =  (a : a) -> b
type f =  () -> b
type f =  () -> Unit

type f =  (a : [a] a, foo, x : bar) -> b
type f =  (foo, a : a) -> b
type f =  (foo, a :  (a) -> b) -> b
type f =  (foo, a :  (a) -> b) ->  () -> Unit

//type f =  (ref foo, ref a :  (ref a) -> b) ->  () -> Unit

type f =  T.() -> Unit
type f =  T.T.() -> Unit
type f =  T<A, B>.T<x>.() -> Unit

type f = [a]  T.() -> Unit
type f = [a]  T.T.() -> Unit
type f = [a]  T<A, B>.T<x>.() -> Unit
