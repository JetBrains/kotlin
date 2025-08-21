// LANGUAGE: +KotlinFunInterfaceConstructorReference

fun interface KSupplier<T> {
    fun get(): T
}

val ks: (() -> String) -> KSupplier<String> =
    ::KSupplier

fun box(): String =
    ks { "OK" }.get()
