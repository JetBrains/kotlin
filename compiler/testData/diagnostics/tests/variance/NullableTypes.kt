// FIR_IDENTICAL
interface In<in I>
interface Out<out O>
interface Inv<P>
fun <T> getT(): T = null!!


interface Test<in I : Any, out O : Any, P : Any> {
    fun ok1(i: I?) : O?
    fun ok2(i: In<O?>?) : Out<O?>?
    fun ok3(i: Inv<in O?>) = getT<Inv<in I?>>()

    fun neOk1(i: <!TYPE_VARIANCE_CONFLICT_ERROR!>O?<!>) : <!TYPE_VARIANCE_CONFLICT_ERROR!>I?<!>
    fun neOk(i: Out<<!TYPE_VARIANCE_CONFLICT_ERROR!>O?<!>>?) : In<<!TYPE_VARIANCE_CONFLICT_ERROR!>O?<!>>?
    fun neOk3(i: Inv<in <!TYPE_VARIANCE_CONFLICT_ERROR!>I?<!>>)
    <!TYPE_VARIANCE_CONFLICT_ERROR!>fun neOk4()<!> = getT<Inv<in O?>?>()
}
