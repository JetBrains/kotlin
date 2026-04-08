// DUMP_IR

fun interface FunInterface {
    suspend operator fun invoke()
}

fun func(f: FunInterface) = Unit

fun box(): String {
    val lambda: () -> Unit = { }
    func(f = lambda)
    func(lambda)
    func {}
    return "OK"
}
