package test

import kotlin.platform.platformStatic

class A {

    default object {
        val b: String = "OK"

        platformStatic fun test1() {
            b
            test2()
            test3()
            "".test4()
        }

        platformStatic fun test2() {
            b
        }

        fun test3() {

        }

        platformStatic fun String.test4() {
            b
        }
    }
}

fun main(args: Array<String>) {
    A.test1()
    A.test2()
    A.test3()
    with(A) {
        A.test1()
    }
}