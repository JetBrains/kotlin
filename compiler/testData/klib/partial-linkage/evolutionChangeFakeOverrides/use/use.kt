package serialization.fake_overrides

class Z: X() {
}

fun test0() = Y().bar()
fun test2() = B().qux()
fun test3() = C().qux()
fun test4() = B().tic()
fun test5() = C().tic()

