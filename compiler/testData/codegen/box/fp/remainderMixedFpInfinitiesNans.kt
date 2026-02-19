// WITH_STDLIB

fun box(): String {
    val aL = 1234567890123456789L
    val rd1 = aL % Double.POSITIVE_INFINITY
    if (rd1 != aL.toDouble()) return "fail1"
    val rd2 = (-aL) % Double.NEGATIVE_INFINITY
    if (rd2 != (-aL).toDouble()) return "fail2"

    val rf1 = aL % Float.POSITIVE_INFINITY
    if (rf1 != aL.toFloat()) return "fail3"
    val rf2 = (-aL) % Float.NEGATIVE_INFINITY
    if (rf2 != (-aL).toFloat()) return "fail4"

    if (!((Double.POSITIVE_INFINITY % 3.0).isNaN())) return "fail5"
    if (!((Float.NEGATIVE_INFINITY % 2.0f).isNaN())) return "fail6"

    if (!((Double.NaN % 1.0).isNaN())) return "fail7"
    if (!((1.0 % Double.NaN).isNaN())) return "fail8"
    if (!((Float.NaN % 1.0f).isNaN())) return "fail9"
    if (!((1.0f % Float.NaN).isNaN())) return "fail0"

    return "OK"
}