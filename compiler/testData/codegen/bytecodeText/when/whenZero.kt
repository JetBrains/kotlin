// IGNORE_BACKEND: JVM_IR
fun test(a: Int, b: Int, c: Int) {
    when (0) {
        a -> throw IllegalArgumentException("a is 0")
        b -> throw IllegalArgumentException("b is 0")
        c -> throw IllegalArgumentException("c is 0")
    }
}

// 0 IF_ICMPNE
// 3 IFNE