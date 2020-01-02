import kotlin.reflect.KProperty

interface In<in T>
interface Out<out T>
interface Inv<T>

class Delegate<T> {
    operator fun getValue(t: Any, p: KProperty<*>): T = null!!
    operator fun setValue(t: Any, p: KProperty<*>, varue: T) {}
}

fun <T> getT(): T = null!!

abstract class Test<in I, out O, P> {
    abstract var type1: I
    abstract var type2: O
    abstract var type3: P
    abstract var type4: In<I>
    abstract var type5: In<O>

    var implicitType1 = getT<I>()
    var implicitType2 = getT<O>()
    var implicitType3 = getT<P>()
    var implicitType4 = getT<In<I>>()
    var implicitType5 = getT<In<O>>()

    var delegateType1 by Delegate<I>()
    var delegateType2 by Delegate<O>()
    var delegateType3 by Delegate<P>()
    var delegateType4 by Delegate<In<I>>()
    var delegateType5 by Delegate<In<O>>()

    abstract var I.receiver1: Int
    abstract var O.receiver2: Int
    abstract var P.receiver3: Int
    abstract var In<I>.receiver4: Int
    abstract var In<O>.receiver5: Int

    var <X : I> X.typeParameter1: Int get() = 0; set(i) {}
    var <X : O> X.typeParameter2: Int get() = 0; set(i) {}
    var <X : P> X.typeParameter3: Int get() = 0; set(i) {}
    var <X : In<I>> X.typeParameter4: Int get() = 0; set(i) {}
    var <X : In<O>> X.typeParameter5: Int get() = 0; set(i) {}

    var <X> X.typeParameter6: Int where X : I get() = 0; set(i) {}
    var <X> X.typeParameter7: Int where X : O get() = 0; set(i) {}
    var <X> X.typeParameter8: Int where X : P get() = 0; set(i) {}
    var <X> X.typeParameter9: Int where X : In<I> get() = 0; set(i) {}
    var <X> X.typeParameter0: Int where X : In<O> get() = 0; set(i) {}
}
