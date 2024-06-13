// LANGUAGE: +ProperIeee754Comparisons

fun box(): String {
    if (-0.0 < 0.0) return "Fail 1"
    if (-0.0 < 0) return "Fail 2"

    return "OK"
}
