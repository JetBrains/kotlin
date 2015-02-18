trait In<in T>
trait Out<out T>
trait Inv<T>

class Delegate<T> {
    fun get(t: Any, p: PropertyMetadata): T = null!!
    fun set(t: Any, p: PropertyMetadata, value: T) {}
}

fun <T> getT(): T = null!!

abstract class Test<in I, out O, P> {
    abstract val type1: <!TYPE_VARIANCE_CONFLICT(I; in; out; I)!>I<!>
    abstract val type2: O
    abstract val type3: P
    abstract val type4: In<I>
    abstract val type5: In<<!TYPE_VARIANCE_CONFLICT(O; out; in; In<O>)!>O<!>>

    <!TYPE_VARIANCE_CONFLICT(I; in; out; I)!>val implicitType1<!> = getT<I>()
    val implicitType2 = getT<O>()
    val implicitType3 = getT<P>()
    val implicitType4 = getT<In<I>>()
    <!TYPE_VARIANCE_CONFLICT(O; out; in; In<O>)!>val implicitType5<!> = getT<In<O>>()

    <!TYPE_VARIANCE_CONFLICT(I; in; out; I)!>val delegateType1<!> by Delegate<I>()
    val delegateType2 by Delegate<O>()
    val delegateType3 by Delegate<P>()
    val delegateType4 by Delegate<In<I>>()
    <!TYPE_VARIANCE_CONFLICT(O; out; in; In<O>)!>val delegateType5<!> by Delegate<In<O>>()

    abstract val I.receiver1: Int
    abstract val <!TYPE_VARIANCE_CONFLICT(O; out; in; O)!>O<!>.receiver2: Int
    abstract val P.receiver3: Int
    abstract val In<<!TYPE_VARIANCE_CONFLICT(I; in; out; In<I>)!>I<!>>.receiver4: Int
    abstract val In<O>.receiver5: Int

    val <X : I> typeParameter1 = 8
    val <X : <!TYPE_VARIANCE_CONFLICT(O; out; in; O)!>O<!>> typeParameter2 = 13
    val <X : P> typeParameter3 = 21
    val <X : In<<!TYPE_VARIANCE_CONFLICT(I; in; out; In<I>)!>I<!>>> typeParameter4 = 34
    val <X : In<O>> typeParameter5 = 55

    val <X> typeParameter6 where X : I = 1
    val <X> typeParameter7 where X : <!TYPE_VARIANCE_CONFLICT(O; out; in; O)!>O<!> = 1
    val <X> typeParameter8 where X : P = 2
    val <X> typeParameter9 where X : In<<!TYPE_VARIANCE_CONFLICT(I; in; out; In<I>)!>I<!>> = 3
    val <X> typeParameter0 where X : In<O> = 5
}