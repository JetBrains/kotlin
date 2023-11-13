// TARGET_BACKEND: JVM_IR

fun increment() {
    var i = 10
    i = i + 10
}

// 1 LINENUMBER 5 L1\n +IINC