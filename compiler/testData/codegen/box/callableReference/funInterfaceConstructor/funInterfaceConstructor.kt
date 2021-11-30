// !LANGUAGE: +AllowKotlinFunInterfaceConstructorReference
// IGNORE_BACKEND: JVM
//  ^ feature supported in IR-based backends only

fun interface KSupplier<T> {
    fun get(): T
}

val ks: (() -> String) -> KSupplier<String> =
    ::KSupplier

fun box(): String =
    ks { "OK" }.get()
