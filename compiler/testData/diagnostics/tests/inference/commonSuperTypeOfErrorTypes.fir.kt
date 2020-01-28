// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST

class Foo<T>
class Bar<S>

fun <T> consume(x: Foo<out T>, y: Foo<out T>) {}
fun <T> materialize() = null as T

fun test() {
    consume(
        materialize<Foo<Bar<ErrorType>>>(),
        materialize<Foo<Bar<ErrorType>>>()
    )

    <!INAPPLICABLE_CANDIDATE!>consume<!>(
        materialize<Foo<Bar<ErrorType>>>(),
        materialize<Foo<ErrorType>>()
    )

}
