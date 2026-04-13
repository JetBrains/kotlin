package pack

interface MyInterface {
    val bar: Int
}

class Impl(override var <caret>bar: Int) : MyInterface
