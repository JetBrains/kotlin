// http://youtrack.jetbrains.com/issue/KT-1996

trait Foo {
    fun foo(): Unit
}

trait Bar {
    fun foo(): Unit
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Baz<!> : Foo, Bar