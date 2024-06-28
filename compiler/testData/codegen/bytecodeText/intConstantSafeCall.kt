// JVM_ABI_K1_K2_DIFF: KT-62485
fun foo() = 10?.toString()

// 0 IFNULL
