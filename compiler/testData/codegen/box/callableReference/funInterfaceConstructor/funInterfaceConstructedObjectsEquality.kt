// LANGUAGE: +KotlinFunInterfaceConstructorReference

// IGNORE_BACKEND: JS_IR, JS_IR_ES6
//  ^ TypeError: tmp is not a function

// FILE: funInterfaceConstructedObjectsEquality.kt

val ks1: (() -> String) -> KSupplier<String> =
    ::KSupplier

val ks11Foo = ks1(::foo)
// getter is used to avoid dependency on lazy initialization support
val ks21Foo get() = ks2(::foo)

fun interface KStringSupplier {
    fun get(): String
}

val kss: (() -> String) -> KStringSupplier =
    ::KStringSupplier

val ks11Bar = ks1(::bar)

val kssFoo = kss(::foo)

fun checkEqual(message: String, a1: Any, a2: Any) {
    if (a1 != a2) {
        throw Exception("$message: equals: $a1 != $a2")
    }
    if (a1.hashCode() != a2.hashCode()) {
        throw Exception("$message: hashCode: ${a1.hashCode()} != ${a2.hashCode()}")
    }
}

fun checkNotEqual(message: String, a1: Any, a2: Any) {
    if (a1 == a2) {
        throw Exception("$message: equals: $a1 == $a2")
    }
}

fun box(): String {
    checkEqual("ks11Foo == ks12Foo (same ctor, different source files)", ks11Foo, ks12Foo)
    checkEqual("ks11Foo == ks21Foo (different ctors, same source file)", ks11Foo, ks21Foo)
    checkEqual("ks11Foo == ks22Foo (different ctors, different source files)", ks11Foo, ks22Foo)

    checkNotEqual("ks11Foo != ks11Bar (different funs)", ks11Foo, ks11Bar)
    checkNotEqual("ks11Foo != kssFoo (different fun interfaces)", ks11Foo, kssFoo)

    return "OK"
}

// FILE: KSupplier.kt

fun interface KSupplier<T> {
    fun get(): T
}

fun foo() = "abc"

fun bar() = "def"

val ks2: (() -> String) -> KSupplier<String> =
    ::KSupplier

val ks12Foo get() = ks1(::foo)
val ks22Foo = ks2(::foo)