// IGNORE_BACKEND: JVM_IR
fun test(): Int {
    val intArray = intArrayOf(1, 2, 3)
    var sum = 0
    for (i in 0..intArray.size - 1) {
        sum += intArray[i]
    }
    return sum
}

// 1 IF_ICMPGE
// 0 IF_ICMPGT
// 0 IF_ICMPEQ
