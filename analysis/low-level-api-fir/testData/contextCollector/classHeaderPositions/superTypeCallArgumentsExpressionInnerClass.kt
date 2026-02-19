package test

class TopLevel {
    open class Base(param: Nested) {
        class Nested
    }

    inner class Child(primaryConstructorParameter: Nested) : Base(<expr>primaryConstructorParameter</expr>)
}