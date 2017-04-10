package test

class A {

    companion object {
        val b: String = "OK"

        @JvmStatic fun test1() {
            b
            test2()
            test3()
            "".test4()
        }

        @JvmStatic fun test2() {
            b
        }

        fun test3() {

        }

        @JvmStatic fun String.test4() {
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