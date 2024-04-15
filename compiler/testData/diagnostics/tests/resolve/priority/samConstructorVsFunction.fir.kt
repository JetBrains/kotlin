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
    consume<Foo<String>>(<!ARGUMENT_TYPE_MISMATCH!>Foo { "" }<!>)
    consume<String>(Foo { "" })
}

fun <T> consume(t: T) {}