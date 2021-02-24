// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
// KJS_WITH_FULL_RUNTIME
// !LANGUAGE: +NewInference
// WITH_RUNTIME

class Foo

class Builder {
    var size: Int = 0

    fun addFoo(foo: Foo): Builder {
        size++
        return this
    }
}

fun box(): String {
    val b = Builder()
    listOf(Foo(), Foo(), Foo()).forEach(b::addFoo)
    return if (b.size == 3) "OK" else "Fail"
}
