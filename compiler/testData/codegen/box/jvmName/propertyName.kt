// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63984, KT-69075

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
