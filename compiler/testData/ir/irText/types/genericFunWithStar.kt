interface IBase
interface IFoo : IBase
interface IBar : IBase

interface I<G> where G : IFoo, G : IBar {}

abstract class Box<T> : IFoo, IBar where T : IFoo, T : IBar  {

    abstract fun <F> foo(tSerializer: I<F>): I<Box<F>> where F : IFoo, F : IBar

    fun bar(vararg serializers: I<*>): I<*> {
        return foo(serializers[0])
    }
}

