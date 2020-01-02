// !DIAGNOSTICS: -UPPER_BOUND_VIOLATED

interface C0<T, S : C0<*, S>>

interface C1<T : C1<T, *>, S : C1<S, *>>   // T -> S, S -> S
interface C2<T : C2<T, *>, S : C2<*, S>>   // T -> S, S -> T

interface D1<T, U> where T : U, U: D1<*, U>
interface D2<T, U> where T : U?, U: D2<*, *>
interface D3<T, U, V> where T : U, U : V, V: D3<*, *, V>

interface A<T, U> where T : A<U, T>, U: A<T, A<in U, T>>