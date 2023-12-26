// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

// expected: rv: 3628800

fun factorial(n: Int): Int {
    var product = 1
    for (i in 1..n) {
        product *= i
    }
    return product
}

val rv = factorial(10)
