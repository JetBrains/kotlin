// ISSUE: KT-70186

interface OutBase<out T> {
    fun foo(): T
}

class BaseImplOut<D : OutBase<Int>>(val a: D) : OutBase<Int> by a

class BaseImplOut2<D>(val a: OutBase<D>) : OutBase<D> by a
class BaseImplOut3<D: Number>(val a: OutBase<D>) : OutBase<D> by a

class BaseImplOut4<D : OutBase<Int>>(val a: D) : OutBase<Int> by a
class BaseImplOut5<D : OutBase<D>>(val a: D) : OutBase<D> by a
class BaseImplOut6<A, D : OutBase<A>>(val a: D) : OutBase<A> by a
class BaseImplOut7<A, D : OutBase<OutBase<A>>>(val a: D) : OutBase<OutBase<A>> by a

class BaseImplOut8<D : OutBase<D>, A: D>(val a: A) : OutBase<D> by a
class BaseImplOut9<out D: Number>(val a: OutBase<D>) : OutBase<D> by a
class BaseImplOut10<out D : OutBase<D>>(val a: D) : OutBase<D> by a
class BaseImplOut11<A: OutBase<*>, D : OutBase<A>>(val a: D) : OutBase<A> by a

fun box() = "OK"