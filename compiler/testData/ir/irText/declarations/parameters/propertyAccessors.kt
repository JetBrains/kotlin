// FIR_IDENTICAL

val test1 get() = 42

var test2 get() = 42; set(value) {}

val String.testExt1 get() = 42

var String.testExt2 get() = 42; set(value) {}

val <T> T.testExt3 get() = 42

var <T> T.testExt4 get() = 42; set(value) {}

class Host<T> {
    val testMem1 get() = 42

    var testMem2 get() = 42; set(value) {}

    val String.testMemExt1 get() = 42

    var String.testMemExt2 get() = 42; set(value) {}

    val <TT> TT.testMemExt3 get() = 42

    var <TT> TT.testMemExt4 get() = 42; set(value) {}
}
