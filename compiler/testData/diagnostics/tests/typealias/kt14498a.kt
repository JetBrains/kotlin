interface Out<out R>
interface Inv<E>
typealias A1<E> = Out<Out<E>>
typealias A2<E> = Out<Out<E>>
typealias A3<E> = Inv<Out<E>>
typealias A4<E> = Out<Inv<E>>

interface Q1<out S> : Out<A1<S>>
interface Q2<out S> : Out<A2<S>>
interface Q3<out S> : Out<<!TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE!>A3<S><!>>
interface Q4<out S> : Out<<!TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE!>A4<S><!>>