// WITH_STDLIB
// FULL_JDK

interface Something

interface Recursive<R> where R : Recursive<R>, R : Something {
    val symbol: AbstractSymbol<R>
}

abstract class AbstractSymbol<E> where E : Recursive<E>, E : Something {
    fun foo(list: List<Any>) {
        val result = list.filterIsInstance<Recursive<*>>().map(Recursive<*>::symbol)
    }
}
