// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

package test

enum class My(val s: String) {
    ENTRY;
    constructor(): this("OK")
}

fun box() = My.ENTRY.s
