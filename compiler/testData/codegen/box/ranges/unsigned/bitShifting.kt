// WITH_STDLIB

fun test(): Int {
    return 1 shl 12
}

fun rest(): UInt {
    return 1u shl 20
}

fun fest(): Long {
    return 1L shl 12
}

fun mest(): ULong {
    return 1uL shl 20
}

fun box(): String = when {
    test() != 4096 -> "fail: test"
    rest() != 1_048_576u -> "fail: rest"
    fest() != 4096L -> "fail: fest"
    mest() != 1_048_576uL -> "fail: mest"
    else -> "OK"
}
