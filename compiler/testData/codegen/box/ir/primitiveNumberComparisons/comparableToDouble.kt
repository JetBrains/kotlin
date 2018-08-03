// !LANGUAGE: +ProperIeee754Comparisons
//IGNORE_BACKEND: JS_IR

fun ltDD(x: Comparable<Double>, y: Double) =
    x is Double && x < y

fun ltCD(x: Comparable<Double>, y: Double) =
    x < y

fun box(): String {
    val Z = 0.0
    val NZ = -0.0

    if (ltDD(NZ, Z)) return "Fail 1"
    if (!ltCD(NZ, Z)) return "Fail 2"

    return "OK"
}