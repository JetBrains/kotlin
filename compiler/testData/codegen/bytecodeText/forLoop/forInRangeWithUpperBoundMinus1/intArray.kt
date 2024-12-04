// TARGET_BACKEND: JVM_IR

fun test() {
    val array = intArrayOf(1, 2, 3)
    var optimized = 0
    for (i in 0..array.size - 1) optimized += array[i]
}

// 0 IF_ICMPGT
// 1 IF_ICMPGE
// 1 IADD
// 0 ISUB
// 4 ILOAD
// 4 ISTORE
// 1 IINC