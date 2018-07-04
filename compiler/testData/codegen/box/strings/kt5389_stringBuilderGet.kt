// IGNORE_BACKEND: JS_IR
fun box(): String {
    val sb = StringBuilder("OK")
    return "${sb.get(0)}${sb[1]}"
}
