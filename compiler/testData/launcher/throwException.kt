package test

// An exception is wrapped several times in this test case to check that stack traces are sanitized
// not only in the outermost exception but in all causes as well

fun f1() {
    throw AssertionError("assert")
}

fun f2() {
    f1()
}

fun f3() {
    f2()
}

fun f4() {
    try {
        f3()
    }
    catch (e: Throwable) {
        throw IllegalStateException("ISE", e)
    }
}

fun f5() {
    f4()
}

fun f6() {
    f5()
}

fun f7() {
    try {
        f6()
    }
    catch (e: Throwable) {
        throw RuntimeException("RE", e)
    }
}

fun f8() {
    f7()
}

fun f9() {
    f8()
}

fun main(args: Array<String>) {
    f9()
}
