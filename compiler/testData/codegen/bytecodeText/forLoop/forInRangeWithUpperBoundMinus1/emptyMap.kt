// TARGET_BACKEND: JVM_IR

fun test() {
    val map = emptyMap<Int, Int>()
    var optimized = ""
    for (i in 0..map.size - 1) optimized += map[i]
}

// 0 IF_ICMPGT
// 1 IF_ICMPGE
// 0 IADD
// 0 ISUB
// 3 ILOAD
// 2 ISTORE
// 1 IINC