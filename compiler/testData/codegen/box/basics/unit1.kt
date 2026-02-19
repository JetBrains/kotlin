fun myPrintln(a: Any): Unit {}

fun box(): String {
    val unit = myPrintln("First")
    if (unit.toString() != "kotlin.Unit") return "FAIL 1: $unit"
    return "OK"
}
