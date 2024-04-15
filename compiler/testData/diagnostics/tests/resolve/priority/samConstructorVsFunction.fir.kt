// FILE: test1.kt
package test1

fun interface Foo<T> {
    fun get(): T
}

fun <T> Foo(value: T): T = value

fun test() {
    consume<Foo<String>>(Foo { "" })
    consume<() -> String>(<!ARGUMENT_TYPE_MISMATCH!>Foo { "" }<!>)
}

fun <T> consume(t: T) {}

// FILE: test2.kt
package test2

fun interface Foo<T> {
    fun get(): T
}

fun <T> Foo(value: () -> T): T = value()

fun test() {
    consume<Foo<String>>(<!OVERLOAD_RESOLUTION_AMBIGUITY!>Foo<!> { "" })
    consume<String>(<!OVERLOAD_RESOLUTION_AMBIGUITY!>Foo<!> { "" })
}

fun <T> consume(t: T) {}