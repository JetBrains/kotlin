package test

class Outer {
    open inner class Base(param: Int)

    inner class Child(primaryConstructorParameter: Int) : Base(<expr>primaryConstructorParameter</expr>) {
        val member: Int = 0
    }
}
