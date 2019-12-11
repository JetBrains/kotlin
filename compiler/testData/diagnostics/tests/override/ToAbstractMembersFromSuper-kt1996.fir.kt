// http://youtrack.jetbrains.com/issue/KT-1996

interface Foo {
    fun foo(): Unit
}

interface Bar {
    fun foo(): Unit
}

class Baz : Foo, Bar