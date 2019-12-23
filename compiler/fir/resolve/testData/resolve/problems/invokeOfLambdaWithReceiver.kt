interface A

fun test(a: A, block: A.() -> Int) {
    a.block()
}

fun A.otherTest(block: A.() -> Int) {
    block()
}

class B {
    fun anotherTest(block: B.() -> Int) {
        block()
    }
}