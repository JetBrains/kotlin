// IGNORE_BACKEND: JVM_IR
enum class AccessMode { READ, WRITE, EXECUTE }

fun whenExpr(access: AccessMode) {
    when (access) {
        AccessMode.READ -> {}
        AccessMode.WRITE -> {}
        AccessMode.EXECUTE -> {}
    }
}

fun box(): String {
    whenExpr(AccessMode.EXECUTE)
    return "OK"
}

// 1 TABLESWITCH
// 0 LOOKUPSWITCH
// 0 ATHROW
