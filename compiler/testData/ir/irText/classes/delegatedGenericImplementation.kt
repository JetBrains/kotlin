interface IBase<A> {
    fun <B> foo(a: A, b: B)
    val <C> C.id: Map<A, C>?
    var <D> List<D>.x: D?
}

class Test1<E>(i: IBase<E>) : IBase<E> by i

class Test2(var j: IBase<String>) : IBase<String> by j
