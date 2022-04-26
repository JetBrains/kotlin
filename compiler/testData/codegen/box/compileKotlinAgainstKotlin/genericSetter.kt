// MODULE: lib
// FILE: 1.kt
var <T> T.prop: Int
    get() = 33
    set(value) {}

// MODULE: main(lib)
// FILE: 2.kt
fun <T> setProp(t: T, v: Int) {
    t.prop = v
}

fun box() = "OK"