// LANGUAGE: +KotlinFunInterfaceConstructorReference

// IGNORE_BACKEND: JS_IR, JS_IR_ES6
//  ^ Failed: ks1 != ks2 (same file, same SAM type)

// FILE: funInterfaceConstructorEquality.kt

val ks1: (() -> String) -> KSupplier<String> =
    ::KSupplier

val ks2: (() -> String) -> KSupplier<String> =
    ::KSupplier

val kn1: (() -> Number) -> KSupplier<Number> =
    ::KSupplier


fun interface KRunnable {
    fun run()
}


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
    checkEqual("ks1 == ks2 (same file, same SAM type)", ks1, ks2)
    checkEqual("ks1 == ks3 (different file, same SAM type)", ks1, ks3)
    checkEqual("ks1 == kn1 (same file, same SAM interface, different type arguments)", ks1, kn1)

    val kr: (() -> Unit) -> KRunnable = ::KRunnable
    checkNotEqual("ks1 != kr (different fun interfaces)", ks1, kr)

    return "OK"
}

// FILE: KSupplier.kt

fun interface KSupplier<T> {
    fun get(): T
}

val ks3: (() -> String) -> KSupplier<String> =
    ::KSupplier
