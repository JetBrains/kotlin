// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

val x = 12

fun foo(y: Int) = x + y

val rv = foo(33)

// expected: rv: 45
