// SKIP_SIGNATURE_DUMP
// ^ K1 generates a useless delegate field for the Test2 class, K2 does not

interface IBase<A> {
    fun <B> foo(a: A, b: B)
    val <C> C.id: Map<A, C>?
    var <D> List<D>.x: D?
}

class Test1<E>(i: IBase<E>) : IBase<E> by i

class Test2(var j: IBase<String>) : IBase<String> by j
