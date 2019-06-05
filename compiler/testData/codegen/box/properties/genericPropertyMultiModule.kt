// WITH_RUNTIME
// MODULE: lib
// FILE: common.kt

class C<T>(var t: T)

var <T> C<T>.live: T
    get() {
        return t
    }
    set(value) {
        t = value
    }

// MODULE: main(lib)
// FILE: main.kt
import kotlin.reflect.KMutableProperty0

fun qux(text: KMutableProperty0<String>): String {
    text.set("OK")
    return text.get()
}

fun box(): String {
    val c = C("FAIL")
    return qux(c::live)
}