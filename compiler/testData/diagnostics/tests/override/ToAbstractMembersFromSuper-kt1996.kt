// http://youtrack.jetbrains.com/issue/KT-1996

interface Foo {
    fun foo(): Unit
}

interface Bar {
    fun foo(): Unit
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Baz<!> : Foo, Bar