// !LANGUAGE: +AllowKotlinFunInterfaceConstructorReference

// IGNORE_BACKEND: JVM
//  ^ unsupported in old JVM BE

// IGNORE_BACKEND: WASM
//  ^ wasm-function[1893]:0x1cf8a: RuntimeError: dereferencing a null pointer

// IGNORE_BACKEND: JS_IR
//  ^ TypeError: tmp is not a function

// FILE: funInterfaceConstructorEquality.kt

val ks1: (() -> String) -> KSupplier<String> =
    ::KSupplier

val ks11Foo = ks1(::foo)
val ks21Foo = ks2(::foo)

fun box(): String {
    if (ks11Foo != ks12Foo)
        return "failed: ks11Foo != ks12Foo (same ctor, different source files)"
    if (ks11Foo != ks21Foo)
        return "failed: ks11Foo != ks21Foo (different ctors, same source file)"
    if (ks11Foo != ks22Foo)
        return "failed: ks11Foo != ks22Foo (different ctors, different source files)"

    return "OK"
}

// FILE: KSupplier.kt

fun interface KSupplier<T> {
    fun get(): T
}

fun foo() = "abc"

val ks2: (() -> String) -> KSupplier<String> =
    ::KSupplier

val ks12Foo = ks1(::foo)
val ks22Foo = ks2(::foo)
