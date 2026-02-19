// ISSUE: KT-70186

interface Base<T: Base<T>> {
    fun foo(a: T): T
}

class BaseImpl<A: Base<A>>(val a: Base<A>) : Base<A> by a
class BaseImpl2<A: Base<A>, D : Base<A>>(val a: D) : Base<A> by a
class BaseImpl3<A: Base<A>, D : Base<*>>(val a: D) : Base<A> by a as Base<A>

fun box() = "OK"