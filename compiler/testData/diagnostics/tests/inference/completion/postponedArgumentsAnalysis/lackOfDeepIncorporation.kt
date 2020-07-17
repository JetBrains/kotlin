// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION -UNCHECKED_CAST -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun <T> materialize(): T = null as T

class Foo<out A> {
    fun <B> product(other: Foo<(A) -> B>) = materialize<Foo<B>>()

    fun <B, R> foo1(other1: Foo<B>, function: (A, B) -> R) {
        val x = product<R>(
            other1.product(
                bar {  b -> { a -> function(<!DEBUG_INFO_EXPRESSION_TYPE("A")!>a<!>, <!DEBUG_INFO_EXPRESSION_TYPE("B")!>b<!>) } }
            )
        )
        <!DEBUG_INFO_EXPRESSION_TYPE("Foo<R>")!>x<!>
    }

    fun <B, C, R> foo2(other1: Foo<B>, other2: Foo<C>, function: (A, B, C) -> R) {
        val x = product<R>(
            other1.product(
                other2.product(
                    bar {  c -> { b -> { a -> function(<!DEBUG_INFO_EXPRESSION_TYPE("A")!>a<!>, <!DEBUG_INFO_EXPRESSION_TYPE("B")!>b<!>, <!DEBUG_INFO_EXPRESSION_TYPE("C")!>c<!>) } } }
                )
            )
        )
        <!DEBUG_INFO_EXPRESSION_TYPE("Foo<R>")!>x<!>
    }

    fun <B, C, D, E, R> foo3(other1: Foo<B>, other2: Foo<C>, other3: Foo<D>, other4: Foo<E>, function: (A, B, C, D) -> R) {
        val x = product<R>(
            other1.product(
                other2.product(
                    other3.product(
                        bar { d -> { c -> { b -> { a -> function(<!DEBUG_INFO_EXPRESSION_TYPE("A")!>a<!>, <!DEBUG_INFO_EXPRESSION_TYPE("B")!>b<!>, <!DEBUG_INFO_EXPRESSION_TYPE("C")!>c<!>, <!DEBUG_INFO_EXPRESSION_TYPE("D")!>d<!>) } } } }
                    )
                )
            )
        )
        <!DEBUG_INFO_EXPRESSION_TYPE("Foo<R>")!>x<!>
    }

    fun <B, C, D, E, R> foo4(other1: Foo<B>, other2: Foo<C>, other3: Foo<D>, other4: Foo<E>, function: (A, B, C, D, E) -> R) {
        val x = product<R>(
            other1.product(
                other2.product(
                    other3.product(
                        other4.product(
                            bar { e -> { d -> { c -> { b -> { a -> function(<!DEBUG_INFO_EXPRESSION_TYPE("A")!>a<!>, <!DEBUG_INFO_EXPRESSION_TYPE("B")!>b<!>, <!DEBUG_INFO_EXPRESSION_TYPE("C")!>c<!>, <!DEBUG_INFO_EXPRESSION_TYPE("D")!>d<!>, <!DEBUG_INFO_EXPRESSION_TYPE("E")!>e<!>) } } } } }
                        )
                    )
                )
            )
        )
        <!DEBUG_INFO_EXPRESSION_TYPE("Foo<R>")!>x<!>
    }

    companion object {
        fun <A> bar(x: A) = materialize<Foo<A>>()
    }
}
