fun intArray() = intArrayOf(0, 0, 0, 0)
fun longArray() = longArrayOf(0, 0, 0, 0)

fun f(): Int {
    var n = 0
    for (i in intArray()) {
        n++
    }

    for (i in longArray()) {
        n++
    }
    return n
}

// 0 iterator
// 1 INVOKESTATIC .*\.intArray \(\)
// 1 INVOKESTATIC .*\.longArray \(\)
// 2 ARRAYLENGTH
// 2 IF_ICMPGE
// 2 IF