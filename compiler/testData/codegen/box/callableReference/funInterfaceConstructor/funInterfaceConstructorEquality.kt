// !LANGUAGE: +AllowKotlinFunInterfaceConstructorReference

// DONT_TARGET_EXACT_BACKEND: JVM
//  ^ old JVM BE generates bogus code that fails in 'invoke', but works almost as expected in terms of equality

// IGNORE_BACKEND: WASM
//  ^ Failed: ks1 != ks2 (same file, same SAM type)

// IGNORE_BACKEND: JS_IR
//  ^ Failed: ks1 != ks2 (same file, same SAM type)

// FILE: funInterfaceConstructorEquality.kt

val ks1: (() -> String) -> KSupplier<String> =
    ::KSupplier

val ks2: (() -> String) -> KSupplier<String> =
    ::KSupplier

val kn1: (() -> Number) -> KSupplier<Number> =
    ::KSupplier

fun box(): String {
    if (ks1 != ks2)
        return "Failed: ks1 != ks2 (same file, same SAM type)"
    if (ks1 != ks3)
        return "Failed: ks1 != ks3 (different file, same SAM type)"
    if (ks1 != kn1)
        return "Failed: ks1 != kn1 (same file, same SAM interface, different type arguments)"

    return "OK"
}

// FILE: KSupplier.kt

fun interface KSupplier<T> {
    fun get(): T
}

val ks3: (() -> String) -> KSupplier<String> =
    ::KSupplier
