enum class AccessMode { READ, WRITE, RW }
fun whenExpr(access: AccessMode) {
    <caret>when (access) {
        AccessMode.READ -> println("read")
        AccessMode.WRITE -> println("write")
        AccessMode.RW -> println("rw")
    }
}
fun println(s: String) {}