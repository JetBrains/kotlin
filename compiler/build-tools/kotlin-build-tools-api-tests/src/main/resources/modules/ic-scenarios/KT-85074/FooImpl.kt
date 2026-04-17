package org.example.foo

fun foo(): Foo {
    return object : Foo {
        override val foo: String = "foo"
    }
}
