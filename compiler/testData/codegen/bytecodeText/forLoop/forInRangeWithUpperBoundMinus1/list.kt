// TARGET_BACKEND: JVM_IR

fun test() {
    val list = listOf(1, 2, 3)
    var optimized = 0
    for (i in 0..list.size - 1) optimized += list[i]
}

// 0 IF_ICMPGT
// 1 IF_ICMPGE
// 1 IADD
// 0 ISUB
// 4 ILOAD
// 4 ISTORE
// 1 IINC