package a

import b.*

class B {
    companion object : A() {}

    init {
        foo()
    }
}

fun box(): String {
    B()
    return result
}