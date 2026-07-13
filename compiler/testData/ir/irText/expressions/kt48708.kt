// ISSUE: KT-48708
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM throws java.lang.Exception instead of kotlin.Exception

fun test(b: Boolean) {
    val x = if (b) {
        3
    } else {
        throw Exception()
        0
    }
    takeInt(x)
}

fun takeInt(x: Int) {}
