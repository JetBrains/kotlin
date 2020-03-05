
fun main() {
    val dmax = Double.MAX_VALUE
    val dmin = Double.MIN_VALUE
    val dnan = Double.NaN
    val dinfp = Double.POSITIVE_INFINITY
    val dinfn = Double.NEGATIVE_INFINITY

    val fmax = Float.MAX_VALUE
    val fmin = Float.MIN_VALUE
    val fnan = Float.NaN
    val finfp = Float.POSITIVE_INFINITY
    val finfn = Float.NEGATIVE_INFINITY
}

// 5 GETSTATIC kotlin/jvm/internal/DoubleCompanionObject.INSTANCE
// 5 GETSTATIC kotlin/jvm/internal/FloatCompanionObject.INSTANCE
// 2 getMAX_VALUE
// 2 getMIN_VALUE
// 2 getNaN
// 2 getPOSITIVE_INFINITY
// 2 getNEGATIVE_INFINITY