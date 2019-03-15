interface In<in I>
interface Out<out O>
interface Inv<P>
fun <T> getT(): T = null!!


interface Test<in I : Any, out O : Any, P : Any> {
    fun ok1(i: I?) : O?
    fun ok2(i: In<O?>?) : Out<O?>?
    fun ok3(i: Inv<in O?>) = getT<Inv<in I?>>()

    fun neOk1(i: <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "O?")!>O?<!>) : <!TYPE_VARIANCE_CONFLICT("I", "in", "out", "I?")!>I?<!>
    fun neOk(i: Out<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "Out<O?>?")!>O?<!>>?) : In<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "In<O?>?")!>O?<!>>?
    fun neOk3(i: Inv<in <!TYPE_VARIANCE_CONFLICT("I", "in", "out", "Inv<in I?>")!>I?<!>>)
    <!TYPE_VARIANCE_CONFLICT("O", "out", "in", "Inv<in O?>?")!>fun neOk4()<!> = getT<Inv<in O?>?>()
}