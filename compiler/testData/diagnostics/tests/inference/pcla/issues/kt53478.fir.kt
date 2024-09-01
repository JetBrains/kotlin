// ISSUE: KT-53478

class UncompilingClass<T : Any>(
    val block: (UncompilingClass<T>.() -> Unit)? = null,
) {

    var uncompilingFun: ((T) -> Unit)? = null
}

fun handleInt(arg: Int) = Unit

fun box() {
    val obj = UncompilingClass {
        uncompilingFun = { handleInt(it) }
    }
}