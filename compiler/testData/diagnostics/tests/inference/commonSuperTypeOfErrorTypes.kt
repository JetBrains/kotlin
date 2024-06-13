// DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST

class Foo<T>
class Bar<S>

fun <T> consume(x: Foo<out T>, y: Foo<out T>) {}
fun <T> materialize() = null as T

fun test() {
    consume(
        materialize<Foo<Bar<<!UNRESOLVED_REFERENCE!>ErrorType<!>>>>(),
        materialize<Foo<Bar<<!UNRESOLVED_REFERENCE!>ErrorType<!>>>>()
    )

    consume(
        materialize<Foo<Bar<<!UNRESOLVED_REFERENCE!>ErrorType<!>>>>(),
        materialize<Foo<<!UNRESOLVED_REFERENCE!>ErrorType<!>>>()
    )

}
