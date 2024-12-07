// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

// param: 10

fun addX(y: Int) = java.lang.Integer.parseInt(args[0]) + y

val rv = addX(3)

// expected: rv: 13
