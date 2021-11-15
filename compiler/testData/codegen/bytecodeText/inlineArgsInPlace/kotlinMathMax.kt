// WITH_STDLIB

fun vectorReductionMax(vA: DoubleArray): Double {
    val n = vA.size
    var x = Double.NEGATIVE_INFINITY
    for (i in 0 until n) {
        x = kotlin.math.max(x, vA[i])
    }
    return x
}

// JVM_IR_TEMPLATES
// Make sure there's no intermediate variable created for 'vA[i]' argument of kotlin.math.max.
// 2 DLOAD
// 2 DSTORE
