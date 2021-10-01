// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// WITH_RUNTIME

fun box(): String {
    val sb = StringBuilder("NK")
    sb[0]++
    return sb.toString()
}
