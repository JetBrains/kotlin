fun intArray() = intArrayOf(0, 0, 0, 0)
fun longArray() = longArrayOf(0, 0, 0, 0)

fun f() {
    for (i in intArray()) {
    }

    for (i in longArray()) {
    }
}

// 0 iterator
// 1 INVOKESTATIC .*\.intArray \(\)
// 1 INVOKESTATIC .*\.longArray \(\)
// 2 ARRAYLENGTH
// 2 IF_ICMP