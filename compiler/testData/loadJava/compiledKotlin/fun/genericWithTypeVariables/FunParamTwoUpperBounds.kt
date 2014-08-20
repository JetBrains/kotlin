package test

trait Foo
trait Bar

fun <T> foo(): Unit
        where T : Foo, T : Bar
    = Unit
