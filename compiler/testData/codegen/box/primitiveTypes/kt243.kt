// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

fun box() : String {
    val t = java.lang.String.copyValueOf(java.lang.String("s").toCharArray())
    val i = java.lang.Integer.MAX_VALUE
    val j = java.lang.Integer.valueOf(15)
    val s = java.lang.String.valueOf(1)
    val l = java.util.Collections.emptyList<Int>()
    return "OK"
}
