package test

interface MyInterface

open class Base {
    class Nested : MyInterface
}

class Child : Base, <expr>MyInterface</expr> by Nested() {
    constructor(): super()
}