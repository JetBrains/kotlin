trait In<in T>
trait Out<out T>
trait Inv<T>

class Delegate<T> {
    fun get(t: Any, p: PropertyMetadata): T = null!!
    fun set(t: Any, p: PropertyMetadata, varue: T) {}
}

fun <T> getT(): T = null!!

abstract class Test<in I, out O, P> {
    abstract var type1: <!TYPE_VARIANCE_CONFLICT(I; in; invariant; I)!>I<!>
    abstract var type2: <!TYPE_VARIANCE_CONFLICT(O; out; invariant; O)!>O<!>
    abstract var type3: P
    abstract var type4: In<<!TYPE_VARIANCE_CONFLICT(I; in; invariant; In<I>)!>I<!>>
    abstract var type5: In<<!TYPE_VARIANCE_CONFLICT(O; out; invariant; In<O>)!>O<!>>

    <!TYPE_VARIANCE_CONFLICT(I; in; invariant; I)!>var implicitType1<!> = getT<I>()
    <!TYPE_VARIANCE_CONFLICT(O; out; invariant; O)!>var implicitType2<!> = getT<O>()
    var implicitType3 = getT<P>()
    <!TYPE_VARIANCE_CONFLICT(I; in; invariant; In<I>)!>var implicitType4<!> = getT<In<I>>()
    <!TYPE_VARIANCE_CONFLICT(O; out; invariant; In<O>)!>var implicitType5<!> = getT<In<O>>()

    <!TYPE_VARIANCE_CONFLICT(I; in; invariant; I)!>var delegateType1<!> by Delegate<I>()
    <!TYPE_VARIANCE_CONFLICT(O; out; invariant; O)!>var delegateType2<!> by Delegate<O>()
    var delegateType3 by Delegate<P>()
    <!TYPE_VARIANCE_CONFLICT(I; in; invariant; In<I>)!>var delegateType4<!> by Delegate<In<I>>()
    <!TYPE_VARIANCE_CONFLICT(O; out; invariant; In<O>)!>var delegateType5<!> by Delegate<In<O>>()

    abstract var I.receiver1: Int
    abstract var <!TYPE_VARIANCE_CONFLICT(O; out; in; O)!>O<!>.receiver2: Int
    abstract var P.receiver3: Int
    abstract var In<<!TYPE_VARIANCE_CONFLICT(I; in; out; In<I>)!>I<!>>.receiver4: Int
    abstract var In<O>.receiver5: Int

    var <X : I> typeParameter1 = 8
    var <X : <!TYPE_VARIANCE_CONFLICT(O; out; in; O)!>O<!>> typeParameter2 = 13
    var <X : P> typeParameter3 = 21
    var <X : In<<!TYPE_VARIANCE_CONFLICT(I; in; out; In<I>)!>I<!>>> typeParameter4 = 34
    var <X : In<O>> typeParameter5 = 55

    var <X> typeParameter6 where X : I = 1
    var <X> typeParameter7 where X : <!TYPE_VARIANCE_CONFLICT(O; out; in; O)!>O<!> = 1
    var <X> typeParameter8 where X : P = 2
    var <X> typeParameter9 where X : In<<!TYPE_VARIANCE_CONFLICT(I; in; out; In<I>)!>I<!>> = 3
    var <X> typeParameter0 where X : In<O> = 5
}