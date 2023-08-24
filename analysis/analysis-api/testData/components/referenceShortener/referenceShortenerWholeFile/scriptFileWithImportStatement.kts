package a.b.c.dependency

import a.b.c.dependency.Foo.Nested

class Foo {
    class Nested
}

fun foo<caret>(): a.b.c.dependency.Foo.Nested = t
