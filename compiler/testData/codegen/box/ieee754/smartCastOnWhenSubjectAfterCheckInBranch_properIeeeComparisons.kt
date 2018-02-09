// !LANGUAGE: +ProperIeee754Comparisons

fun testF(x: Any) =
    when (x) {
        !is Float -> "!Float"
        0.0F -> "0.0"
        else -> "other"
    }

fun testD(x: Any) =
    when (x) {
        !is Double -> "!Double"
        0.0 -> "0.0"
        else -> "other"
    }

fun box(): String {
    val tf = testF(-0.0F)
    if (tf != "0.0") return "Fail 1: $tf"

    val td = testD(-0.0)
    if (td != "0.0") return "Fail 2: $td"

    return "OK"
}