// DUMP_IR
// IGNORE_BACKEND_K1: JVM
// ^K1 with old backend reports FUN_INTERFACE_WITH_SUSPEND_FUNCTION

fun interface FunInterface {
    suspend operator fun invoke()
}

fun func(f: FunInterface) = Unit

fun box(): String {
    val lambda: () -> Unit = { }
    func(f = lambda)
    func(lambda)
    return "OK"
}