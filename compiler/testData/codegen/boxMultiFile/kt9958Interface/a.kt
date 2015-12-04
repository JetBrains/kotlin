package a

import b.*

interface B {
    companion object : A() {}

    fun test() {
        foo()
    }
}

class C : B

fun box(): String {
    C().test()
    return result
}