// FIR_DISABLE_LAZY_RESOLVE_CHECKS
@Deprecated("No")
val f: () -> Unit = {}

fun test() {
    f()
}
