// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// WITH_RUNTIME
// MODULE: lib
// FILE: common.kt

class C<T>(var t: T)
class G<T>(var t: T)

var <T> C<T>.live: T
    get() {
        return t
    }
    set(value) {
        t = value
    }

var <T> G<T>.live: T
    get() {
        return t
    }
    set(value) {
        t = value
    }

// MODULE: main(lib)
// FILE: main.kt
import kotlin.reflect.KMutableProperty0

fun qux(text: KMutableProperty0<String>, s: String): String {
    text.set(s)
    return text.get()
}

fun box(): String {
    val c = C("FAIL_C")
    val g = G("FAIL_G")
    return qux(c::live, "O") + qux(g::live, "K")
}