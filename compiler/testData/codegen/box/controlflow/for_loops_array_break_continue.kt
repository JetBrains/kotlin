// WITH_STDLIB
// DUMP_IR
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM uses java.util.StringBuilder, not kotlin.StringBuilder
import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val intArray = intArrayOf(4, 0, 3, 5)

    val emptyArray = arrayOf<Any>()

    for (element in intArray) {
        sb.append(element)
        if (element == 3) {
            break
        }
    }
    sb.appendLine()
    for (element in emptyArray) {
        sb.append(element)
    }
    sb.appendLine()

    assertEquals("403\n\n", sb.toString())
    return "OK"
}
