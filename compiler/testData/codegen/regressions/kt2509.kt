fun main(args: Array<String>) {
        A()
}

class A: B() {
        override var foo = array<Int?>(12, 13)
}

abstract class B {
        abstract var foo: Array<Int?>
}