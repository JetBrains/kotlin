interface In<in I>
interface Out<out O>
interface Inv<P>
fun <T> getT(): T = null!!


interface Test<in I : Any, out O : Any, P : Any> {
    fun ok1(i: I?) : O?
    fun ok2(i: In<O?>?) : Out<O?>?
    fun ok3(i: Inv<in O?>) = getT<Inv<in I?>>()

    fun neOk1(i: O?) : I?
    fun neOk(i: Out<O?>?) : In<O?>?
    fun neOk3(i: Inv<in I?>)
    fun neOk4() = getT<Inv<in O?>?>()
}