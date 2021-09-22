// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val sb = StringBuilder()
    operator fun String.unaryPlus() {
        sb.append(this)
    }

    +"OK"
    return sb.toString()!!
}
