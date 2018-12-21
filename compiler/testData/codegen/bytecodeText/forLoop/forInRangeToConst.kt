// IGNORE_BACKEND: JVM_IR
const val N = 42

fun test(): Int {
    var sum = 0
    for (i in 1 .. N) {
        sum += i
    }
    return sum
}

// 0 IF_ICMPEQ
// 1 IF_ICMPGT