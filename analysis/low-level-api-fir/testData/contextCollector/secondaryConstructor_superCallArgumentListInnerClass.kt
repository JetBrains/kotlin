package test

class Outer {
    open inner class Base(param: Int)

    inner class Child : Base {
        val member: Int = 0

        constructor(secondaryConstructorParameter: Int) : super<expr>(secondaryConstructorParameter)</expr>
    }
}
