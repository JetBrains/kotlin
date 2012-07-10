// http://youtrack.jetbrains.com/issue/KT-1996

trait Foo {
    fun foo(): Unit
}

trait Bar {
    fun foo(): Unit
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>Baz<!> : Foo, Bar
