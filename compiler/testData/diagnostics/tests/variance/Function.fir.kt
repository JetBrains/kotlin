interface In<in T>
interface Out<out T>
interface Inv<T>
fun <T> getT(): T = null!!

interface Test<in I, out O, P> {
    fun parameters1(i: I, o: O, p: P)
    fun parameters2(i: In<I>)
    fun parameters3(i: In<O>)

    fun explicitReturnType1() : I
    fun explicitReturnType2() : O
    fun explicitReturnType3() : P
    fun explicitReturnType4() : In<I>
    fun explicitReturnType5() : In<O>

    fun imlicitReturnType1() = getT<I>()
    fun imlicitReturnType2() = getT<O>()
    fun imlicitReturnType3() = getT<P>()
    fun imlicitReturnType4() = getT<In<I>>()
    fun imlicitReturnType5() = getT<In<O>>()

    fun I.receiver1()
    fun O.receiver2()
    fun P.receiver3()
    fun In<I>.receiver4()
    fun In<O>.receiver5()

    fun <X : I> typeParameter1()
    fun <X : O> typeParameter2()
    fun <X : P> typeParameter3()
    fun <X : In<I>> typeParameter4()
    fun <X : In<O>> typeParameter5()
}