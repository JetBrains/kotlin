package a.b.c.dependency

class Foo {
    class Nested
}

fun foo<caret>(): a.b.c.dependency.Foo.Nested = t
