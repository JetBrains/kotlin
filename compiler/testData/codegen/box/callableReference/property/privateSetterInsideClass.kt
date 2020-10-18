// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
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
