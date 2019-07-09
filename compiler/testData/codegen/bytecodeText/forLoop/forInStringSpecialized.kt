// IGNORE_BACKEND: JVM_IR
fun test() {
    var s = ""
    for (c in "testString") {
        s += c
    }
}

// 0 iterator
// 0 hasNext
// 0 nextChar
// 0 INVOKEINTERFACE
// 1 ISTORE 4
// 1 ILOAD 4
