// ISSUE: KT-53478
// IGNORE_BACKEND_K1: ANY
// Reason: red code

class UncompilingClass<T : Any>(
    val block: (UncompilingClass<T>.() -> Unit)? = null,
) {

    var uncompilingFun: ((T) -> Unit)? = null
}

fun handleInt(arg: Int) = Unit

fun box(): String {
    val obj = UncompilingClass {
        uncompilingFun = { handleInt(it) }
    }
    return "OK"
}
