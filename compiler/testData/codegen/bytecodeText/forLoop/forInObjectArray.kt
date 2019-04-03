fun array() = arrayOfNulls<Any>(4)

fun f(): Int {
    var n = 0
    for (i in array()) {
        n++
    }
    return n
}

// 0 iterator
// 1 INVOKESTATIC .*\.array \(\)
// 1 ARRAYLENGTH
// 1 IF_ICMPGE
// 1 IF