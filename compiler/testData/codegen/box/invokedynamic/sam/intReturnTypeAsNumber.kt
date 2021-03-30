// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface INum {
    fun get(): Number
}

fun box(): String {
    val num = INum { 42 }
    if (num.get() != 42)
        return "Failed"
    return "OK"
}
