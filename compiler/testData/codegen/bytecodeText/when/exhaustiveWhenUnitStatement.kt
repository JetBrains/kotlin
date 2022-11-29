// IGNORE_BACKEND_K2: JVM_IR

enum class AccessMode { READ, WRITE, EXECUTE }

fun whenExpr(access: AccessMode) {
    when (access) {
        AccessMode.READ -> {}
        AccessMode.WRITE -> {}
        AccessMode.EXECUTE -> {}
    }
}

// 1 TABLESWITCH
// 0 LOOKUPSWITCH
// 0 ATHROW
