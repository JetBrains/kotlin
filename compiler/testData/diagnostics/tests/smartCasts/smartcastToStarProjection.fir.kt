// SKIP_TXT
// ISSUE: KT-55966
// INFERENCE_HELPERS

class Inv<T>

abstract class A<T>

fun <T> get(e: A<T>): T = null!!

fun <K> produce(producer: () -> K): K = null!!

fun <O> test(t: A<O>) {
    @Suppress("UNCHECKED_CAST")
    t as A<Inv<*>>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>")!>id(get(t))<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<*>")!>produce { get(t) }<!>
}
