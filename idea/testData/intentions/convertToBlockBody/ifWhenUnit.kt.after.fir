enum class AccessMode { READ, WRITE, EXECUTE }

fun foo() {}

fun whenExpr(access: AccessMode, arg: Boolean) {
    if (arg) {} else {
        foo()
        when (access) {
            AccessMode.READ -> {}
            AccessMode.WRITE -> {}
            AccessMode.EXECUTE -> {}
        }
    }
}