enum class AccessMode { READ, WRITE, RW }
fun whenExpr(access: AccessMode) {
    <caret>when (access) {
        AccessMode.READ -> println("read")
        AccessMode.WRITE -> println("write")
        else -> println("else")
    }
}
fun println(s: String) {}