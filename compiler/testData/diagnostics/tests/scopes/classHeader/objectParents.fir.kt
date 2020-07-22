interface I<F, G>

val aImpl: A.Interface
    get() = null!!

object A : <!OTHER_ERROR!>Nested<!>(), <!OTHER_ERROR!>Interface<!> by aImpl, <!OTHER_ERROR!>I<Nested, Interface><!> {

    class Nested

    interface Interface
}
