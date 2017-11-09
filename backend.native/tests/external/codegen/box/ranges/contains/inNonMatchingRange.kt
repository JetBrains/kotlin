// WITH_RUNTIME

fun inInt(x: Long): Boolean {
    return x in 1..2
}

fun notInInt(x: Long): Boolean {
    return x !in 1..2
}

fun inLong(x: Int): Boolean {
    return x in 1L..2L
}

fun notInLong(x: Int): Boolean {
    return x !in 1L..2L
}

fun inFloat(x: Double): Boolean {
    return x in 1.0f..2.0f
}

fun notInFloat(x: Double): Boolean {
    return x !in 1.0f..2.0f
}

fun inDouble(x: Float): Boolean {
    return x in 1.0..2.0
}

fun notInDouble(x: Float): Boolean {
    return x !in 1.0..2.0
}

fun box(): String {
    return when {
        !inInt(1L) -> "Fail !inInt"
        inInt(0L) -> "Fail inInt"
        notInInt(1L) -> "Fail notInInt"
        !notInInt(0L) -> "Fail !notInInt"
        !inLong(1) -> "Fail !inLong"
        inLong(0) -> "Fail inLong"
        notInLong(1) -> "Fail notInLong"
        !notInLong(0) -> "Fail !notInLong"
        !inFloat(1.0) -> "Fain !inFloat"
        inFloat(0.0) -> "Fain inFloat"
        notInFloat(1.0) -> "Fail notInFloat"
        !notInFloat(0.0) -> "Fail !notInFloat"
        !inDouble(1.0F) -> "Fail !inDouble"
        inDouble(0.0F) -> "Fail inDouble"
        notInDouble(1.0F) -> "Fail notInDouble"
        !notInDouble(0.0F) -> "Fail !notInDouble"
        else -> "OK"
    }
}