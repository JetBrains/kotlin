package test

open class Base(param: Nested) {
    class Nested
}

class Child(primaryConstructorParameter: Nested) : Base(<expr>primaryConstructorParameter</expr>)