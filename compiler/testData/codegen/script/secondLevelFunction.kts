// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

var x: Int = 0

if (true) {
    fun foo(y: Int) = y + 20
    x = foo(9)
}

val rv = x

// expected: rv: 29
