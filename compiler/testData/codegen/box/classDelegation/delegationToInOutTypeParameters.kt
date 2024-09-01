// ISSUE: KT-70186

interface InOutBase<out T, in K> {
    fun foo(a: K): T
}

class BaseImplInOut<D : InOutBase<Int, Int>>(val a: D) : InOutBase<Int, Int> by a
class BaseImplInOut2<D>(val a: InOutBase<D, D>) : InOutBase<D, D> by a
class BaseImplInOut3<D: Number, K : D>(val a: InOutBase<D, K>) : InOutBase<D, K> by a

class BaseImplInOut4<D : InOutBase<Int, Int>>(val a: D) : InOutBase<Int, Int> by a
class BaseImplInOut5<D : InOutBase<D, K>, K>(val a: D) : InOutBase<D, K> by a
class BaseImplInOut6<A, D : InOutBase<A, D>>(val a: D) : InOutBase<A, D> by a
class BaseImplInOut7<D : InOutBase<D, D>,  A: D>(val a: A) : InOutBase<D, A> by a

class BaseImplInOut8<out D: Number, in A: Number>(val a: InOutBase<D, A>) : InOutBase<D, A> by a
class BaseImplInOut9<A: InOutBase<*, *>, D : InOutBase<A, D>>(val a: D) : InOutBase<A, D> by a

fun box() = "OK"