// IGNORE_BACKEND: JVM_IR
// TODO KT-36650 Don't generate CHECKCAST on null values in JVM_IR
// KT-15411 Unnecessary CHECKCAST bytecode when dealing with null

fun test1(): String? {
    return null
}

fun test2(): BooleanArray? {
    return null
}

fun test3(): Unit? {
    return null
}

// 0 CHECKCAST
