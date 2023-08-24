// FIR_IDENTICAL
open class A<A_T>

open class B<B_T> : A<B_T>() {
    override fun hashCode(): Int = 0
}

open class C<C_T> : B<C_T>()

open class D<D_T> : C<D_T>()
