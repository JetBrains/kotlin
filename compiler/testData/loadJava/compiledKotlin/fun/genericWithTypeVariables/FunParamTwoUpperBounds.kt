package test

interface Foo
interface Bar

fun <T> foo(): Unit
        where T : Foo, T : Bar
    = Unit
