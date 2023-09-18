package test

interface MyInterface

open class Base {
    class Nested : MyInterface
}

class Child(constructorParam: MyInterface) : Base(), MyInterface by <expr>Nested()</expr>