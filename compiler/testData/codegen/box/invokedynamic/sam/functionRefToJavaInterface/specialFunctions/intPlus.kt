// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
//  ^ uses API not implemented on minSdkVersion 19
// IGNORE_BACKEND_FIR: JVM_IR
//  ^ fir2ir creates an IrConst(value=2, type=@EnhancedNullability Int), which
//    the backend generates as an int instead of Integer
// WITH_STDLIB
// JVM_TARGET: 1.8
// FULL_JDK

fun box(): String {
    val map = HashMap<String, Int>()
    map["a"] = 1
    map.merge("a", 2, Int::plus)
    val t = map["a"]
    return if (t == 3) "OK" else "failed: t=$t"
}
