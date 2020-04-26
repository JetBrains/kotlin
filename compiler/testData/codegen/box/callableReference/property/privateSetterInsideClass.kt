import kotlin.reflect.KMutableProperty

class Bar(name: String) {
    var foo: String = name
        private set

    fun test() {
        val p = Bar::foo
        if (p !is KMutableProperty<*>) throw AssertionError("Fail: p is not a KMutableProperty")
        p.set(this, "OK")
    }
}

fun box(): String {
    val bar = Bar("Fail")
    bar.test()
    return bar.foo
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IGNORED_IN_JS

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
