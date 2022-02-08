// TARGET_BACKEND: JVM_IR

fun test() {
    val chars: CharSequence = "123"
    var optimized = ""
    for (i in 0..chars.length - 1) optimized += chars[i]
}

// 0 IF_ICMPGT
// 1 IF_ICMPGE
// 0 IADD
// 0 ISUB
// 3 ILOAD
// 2 ISTORE
// 1 IINC