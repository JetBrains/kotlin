interface In<in T>
interface Out<out T>
interface Inv<T>
fun <T> getT(): T = null!!

interface Test<in I, out O, P> {
    fun parameters1(i: I, o: <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "O")!>O<!>, p: P)
    fun parameters2(i: In<<!TYPE_VARIANCE_CONFLICT("I", "in", "out", "In<I>")!>I<!>>)
    fun parameters3(i: In<O>)

    fun explicitReturnType1() : <!TYPE_VARIANCE_CONFLICT("I", "in", "out", "I")!>I<!>
    fun explicitReturnType2() : O
    fun explicitReturnType3() : P
    fun explicitReturnType4() : In<I>
    fun explicitReturnType5() : In<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "In<O>")!>O<!>>

    <!TYPE_VARIANCE_CONFLICT("I", "in", "out", "I")!>fun imlicitReturnType1()<!> = getT<I>()
    fun imlicitReturnType2() = getT<O>()
    fun imlicitReturnType3() = getT<P>()
    fun imlicitReturnType4() = getT<In<I>>()
    <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "In<O>")!>fun imlicitReturnType5()<!> = getT<In<O>>()

    fun I.receiver1()
    fun <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "O")!>O<!>.receiver2()
    fun P.receiver3()
    fun In<<!TYPE_VARIANCE_CONFLICT("I", "in", "out", "In<I>")!>I<!>>.receiver4()
    fun In<O>.receiver5()

    fun <X : I> typeParameter1()
    fun <X : <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "O")!>O<!>> typeParameter2()
    fun <X : P> typeParameter3()
    fun <X : In<<!TYPE_VARIANCE_CONFLICT("I", "in", "out", "In<I>")!>I<!>>> typeParameter4()
    fun <X : In<O>> typeParameter5()
}