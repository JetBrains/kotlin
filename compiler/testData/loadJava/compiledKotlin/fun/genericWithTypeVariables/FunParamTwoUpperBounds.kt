package test

trait Foo
trait Bar

fun <T> foo()
        where T : Foo, T : Bar
    = Unit.VALUE
