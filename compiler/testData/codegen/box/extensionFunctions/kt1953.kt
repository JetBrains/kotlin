// IGNORE_BACKEND: JS_IR
fun box(): String {
    val sb = StringBuilder()
    operator fun String.unaryPlus() {
        sb.append(this)
    }

    +"OK"
    return sb.toString()!!
}
