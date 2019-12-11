interface I<F, G>

val aImpl: A.Companion.Interface
    get() = null!!

val bImpl: B.Companion.Interface
    get() = null!!

interface A {
    companion object : Nested(), Interface by aImpl, I<Nested, Interface> {

        class Nested

        interface Interface
    }
}

class B {
    companion object : Nested(), Interface by aImpl, I<Nested, Interface> {

        class Nested

        interface Interface
    }
}
