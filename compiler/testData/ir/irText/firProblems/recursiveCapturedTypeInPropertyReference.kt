// WITH_STDLIB
// FULL_JDK
// SKIP_DESERIALIZED_IR_TEXT_DUMP
// REASON: KT-69567 extra @[NoInfer] annotation on type parameter of `fun filterIsInstance<Recursive<*>>()`

interface Something

interface Recursive<R> where R : Recursive<R>, R : Something {
    val symbol: AbstractSymbol<R>
}

abstract class AbstractSymbol<E> where E : Recursive<E>, E : Something {
    fun foo(list: List<Any>) {
        val result = list.filterIsInstance<Recursive<*>>().map(Recursive<*>::symbol)
    }
}
