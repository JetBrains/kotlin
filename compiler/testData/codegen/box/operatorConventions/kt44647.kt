// IGNORE_BACKEND: JVM
// WITH_STDLIB

fun box(): String {
    val sb = StringBuilder("NK")
    sb[0]++
    return sb.toString()
}
