// LANGUAGE: +KotlinFunInterfaceConstructorReference

// IGNORE_BACKEND: JVM
//  ^ unsupported in old JVM BE

fun interface KSupplier<T> {
    fun get(): T
}

val ks: (() -> String) -> KSupplier<String> =
    ::KSupplier

fun box(): String =
    ks { "OK" }.get()
