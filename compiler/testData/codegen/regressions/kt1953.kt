fun box(): String {
    val sb = StringBuilder()
    fun String.plus() {
        sb.append(this)
    }

    +"OK"
    return sb.toString()!!
}
