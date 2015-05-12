interface In<in T>
fun <T> getT(): T = null!!

interface Test<in I, out O, P> {
    fun ok1(i: (O) -> I) : (I) -> O
    fun ok2(i: (P) -> P) : (P) -> P
    fun ok3(i: (In<I>) -> In<O>) = getT<(In<O>) -> In<I>>()

    fun neOk1(i: (<!TYPE_VARIANCE_CONFLICT!>I<!>) -> <!TYPE_VARIANCE_CONFLICT!>O<!>): (<!TYPE_VARIANCE_CONFLICT!>O<!>) -> <!TYPE_VARIANCE_CONFLICT!>I<!>
    fun neOk2(i: (In<<!TYPE_VARIANCE_CONFLICT!>O<!>>) -> In<<!TYPE_VARIANCE_CONFLICT!>I<!>>)
    <!TYPE_VARIANCE_CONFLICT, TYPE_VARIANCE_CONFLICT!>fun neOk3()<!> = getT<(In<I>) -> In<O>>()
}