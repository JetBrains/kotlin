class A {
    inner class Inner(val result: Int)
}

fun box(): String {
    val result = (A::Inner).let { c -> c((::A).let { it() }, 111) }.result + (A::Inner).let { it(A(), 222) }.result
    if (result != 333) return "Fail $result"
    return "OK"
}

// CHECK_BYTECODE_TEXT
// 3 Function[^.\n]*\.invoke
