// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
val Array<String>.firstElement: String get() = get(0)

fun box(): String {
    val p = Array<String>::firstElement
    return p.get(arrayOf("OK", "Fail"))
}
