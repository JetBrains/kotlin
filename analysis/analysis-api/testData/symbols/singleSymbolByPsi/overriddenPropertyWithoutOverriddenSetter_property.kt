package pack

interface MyInterface {
    val bar: Int
}

class Impl : MyInterface {
    override var b<caret>ar: Int = 0
}
