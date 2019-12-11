interface I<F, G>

val aImpl: A.Interface
    get() = null!!

object A : Nested(), Interface by aImpl, I<Nested, Interface> {

    class Nested

    interface Interface
}
