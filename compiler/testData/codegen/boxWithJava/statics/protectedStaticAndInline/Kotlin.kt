package anotherPackage

import First

class Test : First() {

    inline fun doTest(): String {
        return TEST + test()
    }
}

fun box(): String {
    return Test().doTest()
}