// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

// expected: rv: 19
// param: 17 2

val rv = java.lang.Long.parseLong(args[0]) + java.lang.Integer.parseInt(args[1])
