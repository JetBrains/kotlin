typealias f1 =  (T) -> X
// type f1 = {(T) => X}
typealias f2 =  (T, E) -> X
// type f2 = {(T, E) => X}
typealias f_tuple =  (Pair<T, E>) -> X
//type f_tuple = {((T, E)) => X}
typealias hof =   (X) ->  (T) -> Y
//type hof = { (X) => {(T) => Y} }
typealias hof2 =   ( (X) -> Y) ->  (Y) -> Z
//type hof2 = { {(X) => Y} => {(Y) => Z} }


typealias Comparison<in T> =  (a : T, b : T) -> Int
//type Comparison<in T> = {(a : T, b : T) => Int}
typealias Equality<in T> =  (a : T, b : T) -> Boolean
//type Equality<in T> = {(a : T, b : T) => Boolean}
typealias HashFunction<in T> =  (obj : T) -> Int
//type HashFunction<in T> = {(obj : T) => Int}
typealias Runnable =  () -> Unit
//type Runnable = {() => ()}
typealias Function1<in T, out R> =  (input : T) -> R
//type Function1<in T, out R> = {(input : T) => R}


val f1 = {(t : T) : X -> something(t)}
fun f1(t : T) : X = something(t)

val f1 = {(t : T) -> something(t)}
val f1 = {(T) : X -> something(it)}
val f1 = {t -> something(t)}
val f1 = {something(it)}

val f1 :  (T) -> X = {X()}

