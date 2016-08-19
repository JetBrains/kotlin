// IS_APPLICABLE: false
enum class AccessMode { READ, WRITE, RW }
fun whenExpr(mode: Boolean, access: AccessMode) {
    <caret>when (access) {
        AccessMode.READ -> when (mode) {
            true -> println("read")
            false -> println("noread")
        }
        AccessMode.WRITE -> when (mode) {
            true -> println("write")
        }
        AccessMode.RW -> when (mode) {
            true -> println("both")
            else -> println("no both")
        }
    }
}
fun println(s: String) {}