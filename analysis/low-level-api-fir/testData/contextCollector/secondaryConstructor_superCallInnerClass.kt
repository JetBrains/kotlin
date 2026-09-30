package test

class Outer {
    open inner class Base(param: Int)

    inner class Child : Base {
        constructor(secondaryConstructorParameter: Int) : <expr>super(secondaryConstructorParameter)</expr>
    }
}
