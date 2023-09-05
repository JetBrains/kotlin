package test

open class Base(param: Nested) {
    class Nested
}

fun <T> materialize(): T = TODO()

class Child(primaryConstructorParameter: Nested) : Base(materialize<<expr>Nested</expr>>())