// IGNORE_BACKEND_K2: JS_IR
package test

enum class My(val s: String) {
    ENTRY;
    constructor(): this("OK")
}

fun box() = My.ENTRY.s
