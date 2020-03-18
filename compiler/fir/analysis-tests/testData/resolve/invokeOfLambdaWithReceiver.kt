interface A

fun test(a: A, block: A.() -> Int) {
    a.block()
}

interface B

fun B.otherTest(block: B.() -> Int) {
    block()
}

class C {
    fun anotherTest(block: C.() -> Int) {
        block()
    }
}