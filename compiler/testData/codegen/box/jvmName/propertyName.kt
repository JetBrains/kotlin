// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

var v: Int = 1
    @JvmName("vget")
    get
    @JvmName("vset")
    set

fun box(): String {
    v += 1
    if (v != 2) return "Fail: $v"

    return "OK"
}
