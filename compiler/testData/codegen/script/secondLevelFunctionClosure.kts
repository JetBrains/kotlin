// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

val z = 30
var x: Int = 0

if (true) {
    fun foo() = z + 20
    x = foo()
}

val rv = x

// expected: rv: 50
