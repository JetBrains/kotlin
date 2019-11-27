// IGNORE_BACKEND_FIR: JVM_IR
val Array<String>.firstElement: String get() = get(0)

fun box(): String {
    val p = Array<String>::firstElement
    return p.get(arrayOf("OK", "Fail"))
}
