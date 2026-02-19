// ISSUE: KT-70186

interface Base<T> {
    fun foo(a: T): T
}

class BaseImpl(val a: Base<Int>) : Base<Int> by a

class BaseImpl2<D>(val a: Base<D>) : Base<D> by a
class BaseImpl3<D: Number>(val a: Base<D>) : Base<D> by a

class BaseImpl4<D : Base<Int>>(val a: D) : Base<Int> by a
class BaseImpl5<D : Base<D>>(val a: D) : Base<D> by a
class BaseImpl6<A, D : Base<A>>(val a: D) : Base<A> by a
class BaseImpl7<A, D : Base<Base<A>>>(val a: D) : Base<Base<A>> by a

class BaseImpl8<A: Base<*>, D : Base<A>>(val a: D) : Base<A> by a

fun box() = "OK"