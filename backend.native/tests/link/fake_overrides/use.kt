package serialization.fake_overrides

class Z: X() {
}

fun test0() = println(Y().bar())
fun test2() = println(B().qux())
fun test3() = println(C().qux())
