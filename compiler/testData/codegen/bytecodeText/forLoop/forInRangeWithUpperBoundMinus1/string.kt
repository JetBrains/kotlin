// TARGET_BACKEND: JVM_IR

fun test() {
    val str = "123"
    var optimized = ""
    for (i in 0..str.length - 1) optimized += str[i]
}

// 0 IF_ICMPGT
// 1 IF_ICMPGE
// 0 IADD
// 0 ISUB
// 3 ILOAD
// 2 ISTORE
// 1 IINC