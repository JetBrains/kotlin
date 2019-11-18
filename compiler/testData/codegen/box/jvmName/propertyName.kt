// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

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
