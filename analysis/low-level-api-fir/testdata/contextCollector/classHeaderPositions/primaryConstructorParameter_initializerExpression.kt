package test

open class Base {
    class Nested

    val fromBase: String = ""
}

class Child(name: String = <expr>"name"</expr>) : Base()