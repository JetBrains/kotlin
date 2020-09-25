// JVM_TARGET: 9

fun box() {
    val z = "0"
    val result = z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z +
            z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z + z   //200
}

// 1 INVOKEDYNAMIC makeConcatWithConstants
// 0 append
// 0 stringPlus