// PROBLEM: none

enum class AccessMode { READ, WRITE, RW }
fun whenExpr(access: AccessMode) {
    <caret>when (access) {
        AccessMode.READ -> println("read")
        AccessMode.WRITE -> println("write")
    }
}
fun println(s: String) {}