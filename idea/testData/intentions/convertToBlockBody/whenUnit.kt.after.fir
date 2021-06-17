enum class AccessMode { READ, WRITE, EXECUTE }

fun whenExpr(access: AccessMode) {
    when (access) {
        AccessMode.READ -> {}
        AccessMode.WRITE -> {}
        AccessMode.EXECUTE -> {}
    }
}