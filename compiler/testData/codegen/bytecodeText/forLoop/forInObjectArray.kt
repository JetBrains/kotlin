fun array() = arrayOfNulls<Any>(4)

fun f() {
    for (i in array()) {
    }
}

// 0 iterator
// 1 INVOKESTATIC .*\.array \(\)
// 1 ARRAYLENGTH
// 1 IF_ICMP